package su.afk.yummy.tv.data.videodownload.worker

import android.content.Context
import androidx.annotation.OptIn
import androidx.hilt.work.HiltWorker
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
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import su.afk.yummy.tv.data.videodownload.cache.VideoDownloadCacheProvider
import su.afk.yummy.tv.data.videodownload.notification.VideoDownloadNotificationService
import su.afk.yummy.tv.data.videodownload.strategy.DownloadPlayerStrategy
import su.afk.yummy.tv.data.videodownload.strategy.DownloadPlayerStrategyResolver
import su.afk.yummy.tv.data.videodownload.worker.utils.StreamKind
import su.afk.yummy.tv.data.videodownload.worker.utils.USER_AGENT_HEADER
import su.afk.yummy.tv.data.videodownload.worker.utils.isForbiddenHttpResponse
import su.afk.yummy.tv.data.videodownload.worker.utils.isTransientDownloadFailure
import su.afk.yummy.tv.data.videodownload.worker.utils.nextRetryAttempt
import su.afk.yummy.tv.data.videodownload.worker.utils.safeHeaderNames
import su.afk.yummy.tv.data.videodownload.worker.utils.streamKind
import su.afk.yummy.tv.data.videodownload.worker.utils.throttleLabel
import su.afk.yummy.tv.data.videodownload.worker.utils.userAgent
import su.afk.yummy.tv.domain.videodownload.model.VideoDownloadItem
import su.afk.yummy.tv.domain.videodownload.model.VideoDownloadStatus
import su.afk.yummy.tv.domain.videodownload.model.VideoDownloadStreamRefreshResult
import su.afk.yummy.tv.domain.videodownload.repository.VideoDownloadRepository
import su.afk.yummy.tv.domain.videodownload.usecase.RefreshVideoDownloadStreamUseCase
import su.afk.yummy.tv.domain.videodownload.usecase.UpdateVideoDownloadPreparedStreamUseCase
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

@OptIn(UnstableApi::class)
@HiltWorker
class VideoDownloadWorker @AssistedInject internal constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val repository: VideoDownloadRepository,
    private val refreshVideoDownloadStream: RefreshVideoDownloadStreamUseCase,
    private val updateVideoDownloadPreparedStream: UpdateVideoDownloadPreparedStreamUseCase,
    private val strategyResolver: DownloadPlayerStrategyResolver,
    private val cacheProvider: VideoDownloadCacheProvider,
    private val notificationService: VideoDownloadNotificationService,
    private val analytics: VideoDownloadAnalytics,
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val id = inputData.getLong(KEY_DOWNLOAD_ID, 0L).takeIf { it > 0L }
            ?: return Result.failure()
        var item = repository.getDownload(id) ?: return Result.failure()
        val strategy = strategyResolver.resolve(item)
        setForeground(notificationService.createForegroundInfo(item))
        repository.updateStatus(
            id = id,
            status = VideoDownloadStatus.Downloading,
            errorMessage = null,
        )
        if (strategy.shouldRefreshBeforeStart(item, runAttemptCount)) {
            val reason = if (item.progress > 0f && runAttemptCount == 0) {
                DownloadRestartReason.UserResume
            } else {
                DownloadRestartReason.Preflight
            }
            when (
                val result = refreshDownloadStream(
                    id = id,
                    item = item,
                    strategy = strategy,
                    reason = reason
                )
            ) {
                is DownloadStreamRefresh.Success -> item = result.item
                is DownloadStreamRefresh.Failure -> {
                    val failedDetails = "preflightRefreshFailed=${result.message}; " +
                            "workRetry=${runAttemptCount.nextRetryAttempt()}/$MAX_ALLOHA_STREAM_REFRESH_WORK_RETRIES"
                    if (runAttemptCount < MAX_ALLOHA_STREAM_REFRESH_WORK_RETRIES) {
                        logDownloadWarning {
                            "Download id=$id will retry because fresh ${strategy.playerLabel} stream is not ready. " +
                                    "details=$failedDetails"
                        }
                        repository.updateStatus(
                            id = id,
                            status = VideoDownloadStatus.Queued,
                            errorMessage = failedDetails,
                        )
                        return Result.retry()
                    }
                    logDownloadWarning {
                        "Failed download id=$id details=$failedDetails"
                    }
                    repository.updateStatus(
                        id = id,
                        status = VideoDownloadStatus.Failed,
                        errorMessage = failedDetails,
                    )
                    analytics.reportFailed(item, failedDetails)
                    return Result.failure()
                }
            }
        }

        var retriedAfterForbidden = false
        var transientRetryCount = 0
        while (true) {
            val streamKind = item.streamUrl.streamKind()
            try {
                downloadItem(
                    id = id,
                    item = item,
                    strategy = strategy,
                    retriedAfterForbidden = retriedAfterForbidden,
                )
                repository.updateStatus(
                    id = id,
                    status = VideoDownloadStatus.Downloaded,
                    progress = 1f,
                    errorMessage = null,
                )
                analytics.reportSucceeded(item)
                logDownloadInfo { "Completed download id=$id retryUsed=$retriedAfterForbidden" }
                return Result.success()
            } catch (throwable: Throwable) {
                if (throwable is CancellationException) throw throwable
                if (isStopped) {
                    logDownloadInfo { "Stopped download id=$id retryUsed=$retriedAfterForbidden" }
                    return Result.failure()
                }
                val details = throwable.downloadFailureDetails()
                if (
                    streamKind.isAdaptive &&
                    strategy.retriesViaLiveSession &&
                    !retriedAfterForbidden &&
                    throwable.isForbiddenHttpResponse()
                ) {
                    retriedAfterForbidden = true
                    logDownloadWarning(throwable) {
                        "Download id=$id got 403 from live ${strategy.playerLabel} session; reopening session once. " +
                                "details=$details"
                    }
                    delay(TRANSIENT_RETRY_DELAY_MS)
                    continue
                }
                if (
                    streamKind.isAdaptive &&
                    !retriedAfterForbidden &&
                    throwable.isForbiddenHttpResponse()
                ) {
                    retriedAfterForbidden = true
                    logDownloadWarning(throwable) {
                        "Download id=$id got 403 on adaptive stream request; refreshing stream and retrying. " +
                                "details=$details"
                    }
                    when (
                        val refreshResult = refreshDownloadStream(
                            id,
                            item,
                            strategy = strategy,
                            reason = DownloadRestartReason.Forbidden,
                        )
                    ) {
                        is DownloadStreamRefresh.Success -> {
                            item = refreshResult.item
                            continue
                        }

                        is DownloadStreamRefresh.Failure -> {
                            val failedDetails =
                                "$details; refreshFailed=${refreshResult.message}; retryUsed=true"
                            if (
                                strategy.retriesViaLiveSession &&
                                runAttemptCount < MAX_ALLOHA_STREAM_REFRESH_WORK_RETRIES
                            ) {
                                val retryDetails = "$failedDetails; " +
                                        "workRetry=${runAttemptCount.nextRetryAttempt()}/$MAX_ALLOHA_STREAM_REFRESH_WORK_RETRIES"
                                logDownloadWarning(throwable) {
                                    "Download id=$id will retry after expired ${strategy.playerLabel} stream. " +
                                            "details=$retryDetails"
                                }
                                repository.updateStatus(
                                    id = id,
                                    status = VideoDownloadStatus.Queued,
                                    errorMessage = retryDetails,
                                )
                                return Result.retry()
                            }
                            logDownloadWarning(throwable) {
                                "Failed download id=$id details=$failedDetails"
                            }
                            repository.updateStatus(
                                id = id,
                                status = VideoDownloadStatus.Failed,
                                errorMessage = failedDetails,
                            )
                            analytics.reportFailed(item, failedDetails, throwable)
                            return Result.failure()
                        }
                    }
                }

                if (
                    streamKind.isAdaptive &&
                    retriedAfterForbidden &&
                    strategy.retriesViaLiveSession &&
                    throwable.isForbiddenHttpResponse() &&
                    runAttemptCount < MAX_ALLOHA_STREAM_REFRESH_WORK_RETRIES
                ) {
                    val retryDetails = "$details; retryUsed=true; " +
                            "workRetry=${runAttemptCount.nextRetryAttempt()}/$MAX_ALLOHA_STREAM_REFRESH_WORK_RETRIES"
                    logDownloadWarning(throwable) {
                        "Download id=$id will retry because refreshed ${strategy.playerLabel} stream is still forbidden. " +
                                "details=$retryDetails"
                    }
                    repository.updateStatus(
                        id = id,
                        status = VideoDownloadStatus.Queued,
                        errorMessage = retryDetails,
                    )
                    return Result.retry()
                }

                if (
                    streamKind.isAdaptive &&
                    transientRetryCount < MAX_TRANSIENT_DOWNLOAD_RETRIES &&
                    throwable.isTransientDownloadFailure()
                ) {
                    transientRetryCount += 1
                    if (strategy.retriesViaLiveSession) {
                        logDownloadWarning(throwable) {
                            "Download id=$id got transient ${strategy.playerLabel} adaptive failure; " +
                                    "reopening live session before retry. " +
                                    "attempt=$transientRetryCount/$MAX_TRANSIENT_DOWNLOAD_RETRIES " +
                                    "details=$details"
                        }
                    } else {
                        logDownloadWarning(throwable) {
                            "Download id=$id got transient adaptive failure; retrying same stream. " +
                                    "attempt=$transientRetryCount/$MAX_TRANSIENT_DOWNLOAD_RETRIES " +
                                    "details=$details"
                        }
                    }
                    delay(TRANSIENT_RETRY_DELAY_MS)
                    continue
                }

                if (
                    streamKind.isAdaptive &&
                    strategy.retriesViaLiveSession &&
                    throwable.isTransientDownloadFailure() &&
                    runAttemptCount < MAX_ALLOHA_STREAM_REFRESH_WORK_RETRIES
                ) {
                    val retryDetails = "$details; liveSessionRetries=$transientRetryCount; " +
                            "workRetry=${runAttemptCount.nextRetryAttempt()}/" +
                            MAX_ALLOHA_STREAM_REFRESH_WORK_RETRIES
                    logDownloadWarning(throwable) {
                        "Download id=$id exhausted local ${strategy.playerLabel} sessions; scheduling worker retry. " +
                                "details=$retryDetails"
                    }
                    repository.updateStatus(
                        id = id,
                        status = VideoDownloadStatus.Queued,
                        errorMessage = retryDetails,
                    )
                    return Result.retry()
                }

                val failedDetails = "$details; retryUsed=$retriedAfterForbidden"
                logDownloadWarning(throwable) {
                    "Failed download id=$id details=$failedDetails"
                }
                repository.updateStatus(
                    id = id,
                    status = VideoDownloadStatus.Failed,
                    errorMessage = failedDetails,
                )
                analytics.reportFailed(item, failedDetails, throwable)
                return Result.failure()
            }
        }
    }

    private suspend fun refreshDownloadStream(
        id: Long,
        item: VideoDownloadItem,
        strategy: DownloadPlayerStrategy,
        reason: DownloadRestartReason,
    ): DownloadStreamRefresh {
        return when (
            val refreshResult = refreshVideoDownloadStream(
                item = item,
                autoQualityLabel = DEFAULT_AUTO_QUALITY_LABEL,
            )
        ) {
            is VideoDownloadStreamRefreshResult.Success -> {
                val stream = refreshResult.stream
                // The adaptive manifest uses the stable custom key while its signed segment
                // URLs change. Remove only the manifest entry so the next downloader reads the
                // fresh playlist and still reuses already cached segments.
                strategy.manifestKeyToEvictOnRefresh(item)?.let { manifestKey ->
                    runCatching { cacheProvider.cache.removeResource(manifestKey) }
                }
                updateVideoDownloadPreparedStream(id, stream)
                val latestItem = repository.getDownload(id) ?: item
                val refreshedItem = latestItem.copy(
                    streamUrl = stream.url,
                    headers = stream.headers,
                )
                logDownloadInfo {
                    "Refreshed download stream id=$id reason=$reason quality=${stream.qualityLabel} " +
                            "url=${stream.url.safeDownloadUrlForLog()}"
                }
                DownloadStreamRefresh.Success(refreshedItem)
            }

            is VideoDownloadStreamRefreshResult.Failure ->
                DownloadStreamRefresh.Failure(refreshResult.message)
        }
    }

    private suspend fun downloadItem(
        id: Long,
        item: VideoDownloadItem,
        strategy: DownloadPlayerStrategy,
        retriedAfterForbidden: Boolean,
    ) = coroutineScope {
        val streamKind = item.streamUrl.streamKind()
        val liveSession = if (strategy.usesLiveSession(streamKind)) {
            strategy.openLiveSession(item)
                ?: error("${strategy.playerLabel} live session is not ready")
        } else null
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
                            0L
                        )
                    )
                    if (session.expiresAtMs() == expiresAt) {
                        logDownloadInfo { "Refreshing live ${strategy.playerLabel} session id=$id before TTL expiry" }
                        session.refresh()
                    }
                }
            }
        }
        logDownloadInfo {
            "Starting download id=$id animeId=${item.animeId} videoId=${item.videoId} " +
                    "player=${item.playerName} quality=${item.qualityLabel} " +
                    "kind=$streamKind throttle=${streamKind.throttleLabel()} " +
                    "segmentPace=off retryUsed=$retriedAfterForbidden " +
                    "url=${item.streamUrl.safeDownloadUrlForLog()}"
        }
        try {
            withContext(Dispatchers.IO) {
                val downloadHeaders = if (liveSession != null) emptyMap() else
                    strategy.decorateHeaders(item.headers, item.iframeUrl)
                val requestHeaders =
                    downloadHeaders.filterKeys { !it.equals(USER_AGENT_HEADER, ignoreCase = true) }
                val httpUpstream: DataSource.Factory =
                    if (strategy.preferOkHttpUpstream(streamKind)) {
                        OkHttpDataSource.Factory(
                            OkHttpClient.Builder()
                                .connectTimeout(
                                    ADAPTIVE_HTTP_TIMEOUT_MS.toLong(),
                                    TimeUnit.MILLISECONDS
                                )
                                .readTimeout(
                                    ADAPTIVE_HTTP_TIMEOUT_MS.toLong(),
                                    TimeUnit.MILLISECONDS
                                )
                                .build()
                        ).apply {
                            downloadHeaders.userAgent()?.takeIf { it.isNotBlank() }
                                ?.let(::setUserAgent)
                            if (requestHeaders.isNotEmpty()) setDefaultRequestProperties(
                                requestHeaders
                            )
                        }
                    } else {
                        DefaultHttpDataSource.Factory().apply {
                            if (streamKind.isAdaptive) {
                                setConnectTimeoutMs(ADAPTIVE_HTTP_TIMEOUT_MS)
                                setReadTimeoutMs(ADAPTIVE_HTTP_TIMEOUT_MS)
                            }
                            downloadHeaders.userAgent()?.takeIf { it.isNotBlank() }
                                ?.let(::setUserAgent)
                            if (requestHeaders.isNotEmpty()) setDefaultRequestProperties(
                                requestHeaders
                            )
                        }
                    }
                val downloadUrl = liveSession?.initialStream?.url ?: item.streamUrl
                val cacheDataSource = CacheDataSource.Factory()
                    .setCache(cacheProvider.cache)
                    .setUpstreamDataSourceFactory(httpUpstream)
                    .apply {
                        strategy.cacheKeyFactory(item.cacheKey, downloadUrl, streamKind)
                            ?.let(::setCacheKeyFactory)
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
                logDownloadDebug {
                    "Prepared downloader id=$id kind=$streamKind cacheKeyHash=${item.cacheKey.hashCode()} " +
                            "headers=${downloadHeaders.safeHeaderNames()} retryUsed=$retriedAfterForbidden " +
                            "player=${strategy.playerLabel} liveSessionUsed=${liveSession != null}"
                }
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
                        setForegroundAsync(
                            notificationService.createForegroundInfo(
                                item = item,
                                progressPercent = progressPercent,
                            )
                        )
                        setProgressAsync(
                            androidx.work.workDataOf(
                                KEY_PROGRESS to progressPercent,
                            )
                        )
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
                        logDownloadDebug {
                            "Progress id=$id progress=$progressPercent% " +
                                    "bytes=$storedBytesDownloaded total=${storedTotalBytes ?: "unknown"} " +
                                    "retryUsed=$retriedAfterForbidden"
                        }
                    }
                }
            }
        } finally {
            sessionRefreshTimer?.cancel()
            liveSession?.close()
        }
    }

    private fun createDownloader(
        mediaItem: MediaItem,
        cacheDataSourceFactory: CacheDataSource.Factory,
    ): Downloader {
        return when (mediaItem.localConfiguration?.uri?.toString().orEmpty().streamKind()) {
            StreamKind.Hls ->
                HlsDownloader.Factory(cacheDataSourceFactory).create(mediaItem)

            StreamKind.Dash ->
                DashDownloader.Factory(cacheDataSourceFactory).create(mediaItem)

            StreamKind.Progressive -> ProgressiveDownloader(mediaItem, cacheDataSourceFactory)
        }
    }

    private fun String.downloadHost(): String? =
        substringAfter("://", this)
            .substringBefore('/')
            .takeIf { it.isNotBlank() && it != this }

    private sealed interface DownloadStreamRefresh {
        data class Success(val item: VideoDownloadItem) : DownloadStreamRefresh
        data class Failure(val message: String) : DownloadStreamRefresh
    }

    private enum class DownloadRestartReason {
        Preflight,
        Forbidden,
        TransientFailure,
        UserResume,
    }

    companion object {
        const val KEY_DOWNLOAD_ID = "download_id"
        const val KEY_PROGRESS = "progress"
        private const val DEFAULT_AUTO_QUALITY_LABEL = "Auto"
        private const val ADAPTIVE_HTTP_TIMEOUT_MS = 30_000
        private const val MAX_ALLOHA_STREAM_REFRESH_WORK_RETRIES = 3
        private const val MAX_TRANSIENT_DOWNLOAD_RETRIES = 3
        private const val TRANSIENT_RETRY_DELAY_MS = 3_000L
        private const val PROGRESS_LOG_STEP = 10
        private const val ALLOHA_SESSION_REFRESH_LEAD_MS = 20_000L
        private const val ALLOHA_SESSION_EXPIRY_POLL_MS = 500L
    }
}
