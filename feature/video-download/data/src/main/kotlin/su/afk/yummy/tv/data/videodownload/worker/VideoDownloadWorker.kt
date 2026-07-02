package su.afk.yummy.tv.data.videodownload.worker

import android.content.Context
import androidx.annotation.OptIn
import androidx.hilt.work.HiltWorker
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.cache.CacheDataSource
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import su.afk.yummy.tv.data.videodownload.cache.VideoDownloadCacheProvider
import su.afk.yummy.tv.data.videodownload.notification.VideoDownloadNotificationService
import su.afk.yummy.tv.data.videodownload.worker.utils.StreamKind
import su.afk.yummy.tv.data.videodownload.worker.utils.USER_AGENT_HEADER
import su.afk.yummy.tv.data.videodownload.worker.utils.isAllohaDownload
import su.afk.yummy.tv.data.videodownload.worker.utils.isForbiddenHttpResponse
import su.afk.yummy.tv.data.videodownload.worker.utils.isTransientDownloadFailure
import su.afk.yummy.tv.data.videodownload.worker.utils.nextRetryAttempt
import su.afk.yummy.tv.data.videodownload.worker.utils.safeHeaderNames
import su.afk.yummy.tv.data.videodownload.worker.utils.segmentPaceLabel
import su.afk.yummy.tv.data.videodownload.worker.utils.shouldRefreshBeforeDownload
import su.afk.yummy.tv.data.videodownload.worker.utils.streamKind
import su.afk.yummy.tv.data.videodownload.worker.utils.throttleLabel
import su.afk.yummy.tv.data.videodownload.worker.utils.userAgent
import su.afk.yummy.tv.data.videodownload.worker.utils.withDownloadRequestHeaders
import su.afk.yummy.tv.domain.videodownload.model.VideoDownloadItem
import su.afk.yummy.tv.domain.videodownload.model.VideoDownloadStatus
import su.afk.yummy.tv.domain.videodownload.model.VideoDownloadStreamRefreshResult
import su.afk.yummy.tv.domain.videodownload.repository.VideoDownloadRepository
import su.afk.yummy.tv.domain.videodownload.usecase.RefreshVideoDownloadStreamUseCase
import su.afk.yummy.tv.domain.videodownload.usecase.UpdateVideoDownloadPreparedStreamUseCase
import kotlin.math.roundToInt

@OptIn(UnstableApi::class)
@HiltWorker
class VideoDownloadWorker @AssistedInject internal constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val repository: VideoDownloadRepository,
    private val refreshVideoDownloadStream: RefreshVideoDownloadStreamUseCase,
    private val updateVideoDownloadPreparedStream: UpdateVideoDownloadPreparedStreamUseCase,
    private val cacheProvider: VideoDownloadCacheProvider,
    private val notificationService: VideoDownloadNotificationService,
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val id = inputData.getLong(KEY_DOWNLOAD_ID, 0L).takeIf { it > 0L }
            ?: return Result.failure()
        var item = repository.getDownload(id) ?: return Result.failure()
        setForeground(notificationService.createForegroundInfo(item))
        repository.updateStatus(
            id = id,
            status = VideoDownloadStatus.Downloading,
            errorMessage = null,
        )
        if (item.shouldRefreshBeforeDownload(runAttemptCount)) {
            when (val result = refreshDownloadStream(id = id, item = item, reason = "preflight")) {
                is DownloadStreamRefresh.Success -> item = result.item
                is DownloadStreamRefresh.Failure -> {
                    val failedDetails = "preflightRefreshFailed=${result.message}; " +
                            "workRetry=${runAttemptCount.nextRetryAttempt()}/$MAX_ALLOHA_STREAM_REFRESH_WORK_RETRIES"
                    if (runAttemptCount < MAX_ALLOHA_STREAM_REFRESH_WORK_RETRIES) {
                        logDownloadWarning {
                            "Download id=$id will retry because fresh Alloha stream is not ready. " +
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
                    return Result.failure()
                }
            }
        }

        var retriedAfterForbidden = false
        var transientRetryCount = 0
        while (true) {
            val streamKind = item.streamUrl.streamKind()
            try {
                downloadItem(id = id, item = item, retriedAfterForbidden = retriedAfterForbidden)
                repository.updateStatus(
                    id = id,
                    status = VideoDownloadStatus.Downloaded,
                    progress = 1f,
                    errorMessage = null,
                )
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
                    !retriedAfterForbidden &&
                    throwable.isForbiddenHttpResponse()
                ) {
                    retriedAfterForbidden = true
                    logDownloadWarning(throwable) {
                        "Download id=$id got 403 on adaptive stream request; refreshing stream and retrying. " +
                                "details=$details"
                    }
                    when (val refreshResult = refreshDownloadStream(id, item, reason = "403")) {
                        is DownloadStreamRefresh.Success -> {
                            item = refreshResult.item
                            continue
                        }

                        is DownloadStreamRefresh.Failure -> {
                            val failedDetails =
                                "$details; refreshFailed=${refreshResult.message}; retryUsed=true"
                            if (
                                item.isAllohaDownload() &&
                                runAttemptCount < MAX_ALLOHA_STREAM_REFRESH_WORK_RETRIES
                            ) {
                                val retryDetails = "$failedDetails; " +
                                        "workRetry=${runAttemptCount.nextRetryAttempt()}/$MAX_ALLOHA_STREAM_REFRESH_WORK_RETRIES"
                                logDownloadWarning(throwable) {
                                    "Download id=$id will retry after expired Alloha stream. " +
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
                            return Result.failure()
                        }
                    }
                }

                if (
                    streamKind.isAdaptive &&
                    retriedAfterForbidden &&
                    item.isAllohaDownload() &&
                    throwable.isForbiddenHttpResponse() &&
                    runAttemptCount < MAX_ALLOHA_STREAM_REFRESH_WORK_RETRIES
                ) {
                    val retryDetails = "$details; retryUsed=true; " +
                            "workRetry=${runAttemptCount.nextRetryAttempt()}/$MAX_ALLOHA_STREAM_REFRESH_WORK_RETRIES"
                    logDownloadWarning(throwable) {
                        "Download id=$id will retry because refreshed Alloha stream is still forbidden. " +
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
                    if (item.isAllohaDownload()) {
                        logDownloadWarning(throwable) {
                            "Download id=$id got transient Alloha adaptive failure; " +
                                    "refreshing stream before retry. " +
                                    "attempt=$transientRetryCount/$MAX_TRANSIENT_DOWNLOAD_RETRIES " +
                                    "details=$details"
                        }
                        when (
                            val refreshResult = refreshDownloadStream(
                                id = id,
                                item = item,
                                reason = "transient",
                            )
                        ) {
                            is DownloadStreamRefresh.Success -> {
                                val currentHost = item.streamUrl.downloadHost()
                                val refreshedHost = refreshResult.item.streamUrl.downloadHost()
                                if (
                                    (refreshResult.item.streamUrl == item.streamUrl ||
                                            refreshedHost == currentHost && refreshedHost != null)
                                ) {
                                    val retryDetails = "$details; refreshedSameHost=true; " +
                                            "workRetry=${runAttemptCount.nextRetryAttempt()}"
                                    logDownloadWarning(throwable) {
                                        "Download id=$id will retry because refreshed Alloha stream " +
                                                "still points to the same transiently unreachable host. " +
                                                "details=$retryDetails"
                                    }
                                    repository.updateStatus(
                                        id = id,
                                        status = VideoDownloadStatus.Queued,
                                        errorMessage = retryDetails,
                                    )
                                    return Result.retry()
                                }
                                item = refreshResult.item
                                delay(TRANSIENT_RETRY_DELAY_MS)
                                continue
                            }

                            is DownloadStreamRefresh.Failure -> {
                                logDownloadWarning(throwable) {
                                    "Download id=$id could not refresh transient Alloha stream; " +
                                            "retrying same stream. " +
                                            "attempt=$transientRetryCount/$MAX_TRANSIENT_DOWNLOAD_RETRIES " +
                                            "details=$details; refreshFailed=${refreshResult.message}"
                                }
                            }
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

                val failedDetails = "$details; retryUsed=$retriedAfterForbidden"
                logDownloadWarning(throwable) {
                    "Failed download id=$id details=$failedDetails"
                }
                repository.updateStatus(
                    id = id,
                    status = VideoDownloadStatus.Failed,
                    errorMessage = failedDetails,
                )
                return Result.failure()
            }
        }
    }

    private suspend fun refreshDownloadStream(
        id: Long,
        item: VideoDownloadItem,
        reason: String,
    ): DownloadStreamRefresh {
        return when (
            val refreshResult = refreshVideoDownloadStream(
                item = item,
                autoQualityLabel = DEFAULT_AUTO_QUALITY_LABEL,
            )
        ) {
            is VideoDownloadStreamRefreshResult.Success -> {
                val stream = refreshResult.stream
                updateVideoDownloadPreparedStream(id, stream)
                val latestItem = repository.getDownload(id) ?: item
                val refreshedItem = latestItem.copy(
                    qualityLabel = stream.qualityLabel,
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
        retriedAfterForbidden: Boolean,
    ) {
        val streamKind = item.streamUrl.streamKind()
        logDownloadInfo {
            "Starting download id=$id animeId=${item.animeId} videoId=${item.videoId} " +
                    "player=${item.playerName} quality=${item.qualityLabel} " +
                    "kind=$streamKind throttle=${streamKind.throttleLabel()} " +
                    "segmentPace=${streamKind.segmentPaceLabel()} retryUsed=$retriedAfterForbidden " +
                    "url=${item.streamUrl.safeDownloadUrlForLog()}"
        }
        withContext(Dispatchers.IO) {
            val downloadHeaders = item.headers.withDownloadRequestHeaders(item.iframeUrl)
            val httpUpstream = DefaultHttpDataSource.Factory().apply {
                if (streamKind.isAdaptive) {
                    setConnectTimeoutMs(ADAPTIVE_HTTP_TIMEOUT_MS)
                    setReadTimeoutMs(ADAPTIVE_HTTP_TIMEOUT_MS)
                }
                downloadHeaders.userAgent()?.takeIf { it.isNotBlank() }?.let(::setUserAgent)
                val requestHeaders =
                    downloadHeaders.filterKeys { !it.equals(USER_AGENT_HEADER, ignoreCase = true) }
                if (requestHeaders.isNotEmpty()) setDefaultRequestProperties(requestHeaders)
            }
            val cacheDataSource = CacheDataSource.Factory()
                .setCache(cacheProvider.cache)
                .setUpstreamDataSourceFactory(httpUpstream)
            val mediaItem = MediaItem.Builder()
                .setUri(item.streamUrl)
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
                        "headers=${downloadHeaders.safeHeaderNames()} retryUsed=$retriedAfterForbidden"
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

    companion object {
        const val KEY_DOWNLOAD_ID = "download_id"
        const val KEY_PROGRESS = "progress"
        private const val DEFAULT_AUTO_QUALITY_LABEL = "Auto"
        private const val ADAPTIVE_HTTP_TIMEOUT_MS = 30_000
        private const val MAX_ALLOHA_STREAM_REFRESH_WORK_RETRIES = 3
        private const val MAX_TRANSIENT_DOWNLOAD_RETRIES = 3
        private const val TRANSIENT_RETRY_DELAY_MS = 3_000L
        private const val PROGRESS_LOG_STEP = 10
    }
}
