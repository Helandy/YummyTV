package su.afk.yummy.tv.data.videodownload.engine

import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.dash.offline.DashDownloader
import androidx.media3.exoplayer.hls.offline.HlsDownloader
import androidx.media3.exoplayer.offline.Downloader
import androidx.media3.exoplayer.offline.ProgressiveDownloader
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.runInterruptible
import okhttp3.OkHttpClient
import su.afk.yummy.tv.core.analytics.api.AnalyticsTracker
import su.afk.yummy.tv.data.videodownload.cache.VideoDownloadCacheProvider
import su.afk.yummy.tv.data.videodownload.cache.downloadCacheKeyFactoryFor
import su.afk.yummy.tv.data.videodownload.strategy.DownloadPlayerStrategy
import su.afk.yummy.tv.data.videodownload.worker.RateLimitedDataSource
import su.afk.yummy.tv.data.videodownload.worker.logDownloadDebug
import su.afk.yummy.tv.data.videodownload.worker.logDownloadInfo
import su.afk.yummy.tv.data.videodownload.worker.safeDownloadUrlForLog
import su.afk.yummy.tv.data.videodownload.worker.utils.StreamKind
import su.afk.yummy.tv.data.videodownload.worker.utils.USER_AGENT_HEADER
import su.afk.yummy.tv.data.videodownload.worker.utils.safeHeaderNames
import su.afk.yummy.tv.data.videodownload.worker.utils.streamKind
import su.afk.yummy.tv.data.videodownload.worker.utils.throttleLabel
import su.afk.yummy.tv.data.videodownload.worker.utils.userAgent
import su.afk.yummy.tv.domain.videodownload.model.VideoDownloadItem
import su.afk.yummy.tv.domain.videodownload.model.VideoDownloadStatus
import su.afk.yummy.tv.domain.videodownload.repository.VideoDownloadRepository
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToInt

/**
 * Одна попытка скачать поток серии в кэш загрузок: собирает upstream, держит живую сессию
 * балансера и пишет прогресс в хранилище. Ретраи и перерезолв потока решает воркер.
 */
@OptIn(UnstableApi::class)
@Singleton
internal class VideoDownloadExecutor @Inject constructor(
    private val repository: VideoDownloadRepository,
    private val cacheProvider: VideoDownloadCacheProvider,
    private val analyticsTracker: AnalyticsTracker,
) {
    // Один клиент на все загрузки: общий пул соединений вместо нового клиента на каждую попытку
    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(ADAPTIVE_HTTP_TIMEOUT_MS.toLong(), TimeUnit.MILLISECONDS)
            .readTimeout(ADAPTIVE_HTTP_TIMEOUT_MS.toLong(), TimeUnit.MILLISECONDS)
            .build()
    }

    /**
     * Качает [item] до конца или бросает исключение. Отмена корутины прерывает блокирующий
     * [Downloader.download]: зовёт [Downloader.cancel] и прерывает поток загрузки.
     */
    suspend fun download(
        id: Long,
        item: VideoDownloadItem,
        strategy: DownloadPlayerStrategy,
        retriedAfterForbidden: Boolean,
        onProgressPercent: (Int) -> Unit,
    ) = coroutineScope {
        val streamKind = item.streamUrl.streamKind()
        val liveSession = if (strategy.usesLiveSession(streamKind)) {
            strategy.openLiveSession(item)
                ?: error("${strategy.playerLabel} live session is not ready")
        } else {
            null
        }
        val sessionRefreshTimer = liveSession?.let { session ->
            launch {
                while (true) {
                    val expiresAt = session.expiresAtMs()
                    if (expiresAt == null) {
                        delay(ALLOHA_SESSION_EXPIRY_POLL_MS)
                        continue
                    }
                    delay(
                        (expiresAt - System.currentTimeMillis() - ALLOHA_SESSION_REFRESH_LEAD_MS).coerceAtLeast(
                            0L,
                        ),
                    )
                    if (session.expiresAtMs() == expiresAt) {
                        analyticsTracker.logDownloadInfo { "Refreshing live ${strategy.playerLabel} session id=$id before TTL expiry" }
                        session.refresh()
                    }
                }
            }
        }
        analyticsTracker.logDownloadInfo {
            "Starting download id=$id animeId=${item.animeId} videoId=${item.videoId} " +
                "player=${item.playerName} quality=${item.qualityLabel} " +
                "kind=$streamKind throttle=${streamKind.throttleLabel()} " +
                "rateLimit=${strategy.downloadBytesPerSecond?.let { "${it / (1024 * 1024)}MB/s" } ?: "off"} " +
                "retryUsed=$retriedAfterForbidden " +
                "url=${item.streamUrl.safeDownloadUrlForLog()}"
        }
        var cancellationWatcher: Job? = null
        try {
            val downloadHeaders = if (liveSession != null) {
                emptyMap()
            } else {
                strategy.decorateHeaders(item.headers, item.iframeUrl)
            }
            val requestHeaders =
                downloadHeaders.filterKeys { !it.equals(USER_AGENT_HEADER, ignoreCase = true) }
            val httpUpstream = createHttpUpstream(
                strategy = strategy,
                streamKind = streamKind,
                downloadHeaders = downloadHeaders,
                requestHeaders = requestHeaders,
            )
            val throttledUpstream = strategy.downloadBytesPerSecond
                ?.let { RateLimitedDataSource.Factory(httpUpstream, it) }
                ?: httpUpstream
            val downloadUrl = liveSession?.initialStream?.url ?: item.streamUrl
            val cacheDataSource = CacheDataSource.Factory()
                .setCache(cacheProvider.cache)
                .setUpstreamDataSourceFactory(throttledUpstream)
                .apply {
                    downloadCacheKeyFactoryFor(
                        scheme = item.cacheKeyScheme,
                        downloadCacheKey = item.cacheKey,
                        manifestUri = downloadUrl,
                        legacyRotating = strategy.usesRotatingSegmentUrls(streamKind),
                    )?.let(::setCacheKeyFactory)
                }
            val mediaItem = MediaItem.Builder()
                .setUri(downloadUrl)
                .setCustomCacheKey(item.cacheKey)
                .build()
            val savedItem = repository.getDownload(id) ?: item
            var progressFloor = savedItem.progress.coerceIn(0f, 1f)
            var bytesFloor = savedItem.bytesDownloaded
            var totalSnapshot = savedItem.totalBytes
            var lastProgress = -1
            var lastLoggedProgress = -1
            val downloader = createDownloader(mediaItem, cacheDataSource)
            analyticsTracker.logDownloadDebug {
                "Prepared downloader id=$id kind=$streamKind cacheKeyHash=${item.cacheKey.hashCode()} " +
                    "headers=${downloadHeaders.safeHeaderNames()} retryUsed=$retriedAfterForbidden " +
                    "player=${strategy.playerLabel} liveSessionUsed=${liveSession != null}"
            }
            val finished = AtomicBoolean(false)
            // Media3 требует cancel() и прерывание потока: сам download() блокирующий и отмену корутины
            // не видит. runInterruptible прерывает поток, а этот сторож зовёт cancel().
            cancellationWatcher = launch(start = CoroutineStart.UNDISPATCHED) {
                try {
                    awaitCancellation()
                } finally {
                    if (!finished.get()) downloader.cancel()
                }
            }
            runInterruptible(Dispatchers.IO) {
                downloader.download { contentLength, bytesDownloaded, percentDownloaded ->
                    val total = contentLength.takeIf { it > 0L }
                    val reportedProgress = if (percentDownloaded >= 0f) {
                        (percentDownloaded / 100f).coerceIn(0f, 1f)
                    } else if (total != null) {
                        (bytesDownloaded.toFloat() / total.toFloat()).coerceIn(0f, 1f)
                    } else {
                        0f
                    }
                    if (reportedProgress > progressFloor) {
                        progressFloor = reportedProgress
                    }
                    if (bytesDownloaded > bytesFloor) {
                        bytesFloor = bytesDownloaded
                    }
                    if (total != null) {
                        totalSnapshot = total
                    }
                    val progress = progressFloor
                    val storedBytesDownloaded = bytesFloor
                    val storedTotalBytes = totalSnapshot
                    val progressPercent = (progress * 100).roundToInt()
                    if (progressPercent != lastProgress) {
                        lastProgress = progressPercent
                        onProgressPercent(progressPercent)
                        runBlocking {
                            repository.updateStatus(
                                id = id,
                                status = VideoDownloadStatus.Downloading,
                                progress = progress,
                                bytesDownloaded = storedBytesDownloaded,
                                totalBytes = storedTotalBytes,
                                errorMessage = null,
                            )
                        }
                    }
                    if (progressPercent / PROGRESS_LOG_STEP > lastLoggedProgress / PROGRESS_LOG_STEP) {
                        lastLoggedProgress = progressPercent
                        analyticsTracker.logDownloadDebug {
                            "Progress id=$id progress=$progressPercent% " +
                                "bytes=$storedBytesDownloaded total=${storedTotalBytes ?: "unknown"} " +
                                "retryUsed=$retriedAfterForbidden"
                        }
                    }
                }
                finished.set(true)
            }
        } finally {
            cancellationWatcher?.cancel()
            sessionRefreshTimer?.cancel()
            liveSession?.close()
        }
    }

    private fun createHttpUpstream(
        strategy: DownloadPlayerStrategy,
        streamKind: StreamKind,
        downloadHeaders: Map<String, String>,
        requestHeaders: Map<String, String>,
    ): DataSource.Factory {
        val userAgent = downloadHeaders.userAgent()?.takeIf { it.isNotBlank() }
        return if (strategy.preferOkHttpUpstream(streamKind)) {
            OkHttpDataSource.Factory(okHttpClient).apply {
                userAgent?.let(::setUserAgent)
                if (requestHeaders.isNotEmpty()) setDefaultRequestProperties(requestHeaders)
            }
        } else {
            DefaultHttpDataSource.Factory().apply {
                if (streamKind.isAdaptive) {
                    setConnectTimeoutMs(ADAPTIVE_HTTP_TIMEOUT_MS)
                    setReadTimeoutMs(ADAPTIVE_HTTP_TIMEOUT_MS)
                }
                userAgent?.let(::setUserAgent)
                if (requestHeaders.isNotEmpty()) setDefaultRequestProperties(requestHeaders)
            }
        }
    }

    private fun createDownloader(
        mediaItem: MediaItem,
        cacheDataSourceFactory: CacheDataSource.Factory,
    ): Downloader = when (mediaItem.localConfiguration?.uri?.toString().orEmpty().streamKind()) {
        StreamKind.Hls ->
            HlsDownloader.Factory(cacheDataSourceFactory).create(mediaItem)

        StreamKind.Dash ->
            DashDownloader.Factory(cacheDataSourceFactory).create(mediaItem)

        StreamKind.Progressive -> ProgressiveDownloader(mediaItem, cacheDataSourceFactory)
    }

    private companion object {
        const val ADAPTIVE_HTTP_TIMEOUT_MS = 30_000
        const val PROGRESS_LOG_STEP = 10
        const val ALLOHA_SESSION_REFRESH_LEAD_MS = 20_000L
        const val ALLOHA_SESSION_EXPIRY_POLL_MS = 500L
    }
}
