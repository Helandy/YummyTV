package su.afk.yummy.tv.data.videodownload.worker

import android.content.Context
import androidx.annotation.OptIn
import androidx.hilt.work.HiltWorker
import androidx.media3.common.util.UnstableApi
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import su.afk.yummy.tv.core.analytics.api.AnalyticsTracker
import su.afk.yummy.tv.data.videodownload.R
import su.afk.yummy.tv.data.videodownload.cache.VideoDownloadCacheProvider
import su.afk.yummy.tv.data.videodownload.engine.DownloadIntegrityVerifier
import su.afk.yummy.tv.data.videodownload.engine.VideoDownloadExecutor
import su.afk.yummy.tv.data.videodownload.notification.VideoDownloadNotificationService
import su.afk.yummy.tv.data.videodownload.strategy.DownloadPlayerStrategy
import su.afk.yummy.tv.data.videodownload.strategy.DownloadPlayerStrategyResolver
import su.afk.yummy.tv.data.videodownload.worker.utils.StreamKind
import su.afk.yummy.tv.data.videodownload.worker.utils.isForbiddenHttpResponse
import su.afk.yummy.tv.data.videodownload.worker.utils.isOkCdnNodeRefusal
import su.afk.yummy.tv.data.videodownload.worker.utils.isTransientDownloadFailure
import su.afk.yummy.tv.data.videodownload.worker.utils.nextRetryAttempt
import su.afk.yummy.tv.data.videodownload.worker.utils.setForegroundIfAllowed
import su.afk.yummy.tv.data.videodownload.worker.utils.streamKind
import su.afk.yummy.tv.domain.videodownload.model.VideoDownloadItem
import su.afk.yummy.tv.domain.videodownload.model.VideoDownloadStatus
import su.afk.yummy.tv.domain.videodownload.model.VideoDownloadStreamRefreshResult
import su.afk.yummy.tv.domain.videodownload.repository.VideoDownloadExportRepository
import su.afk.yummy.tv.domain.videodownload.repository.VideoDownloadRepository
import su.afk.yummy.tv.domain.videodownload.repository.VideoDownloadStreamRefreshRepository
import java.security.MessageDigest

@OptIn(UnstableApi::class)
@HiltWorker
class VideoDownloadWorker @AssistedInject internal constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val repository: VideoDownloadRepository,
    private val exportRepository: VideoDownloadExportRepository,
    private val streamRefresher: VideoDownloadStreamRefreshRepository,
    private val strategyResolver: DownloadPlayerStrategyResolver,
    private val cacheProvider: VideoDownloadCacheProvider,
    private val executor: VideoDownloadExecutor,
    private val integrityVerifier: DownloadIntegrityVerifier,
    private val notificationService: VideoDownloadNotificationService,
    private val analytics: VideoDownloadAnalytics,
    private val analyticsTracker: AnalyticsTracker,
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val id = inputData.getLong(KEY_DOWNLOAD_ID, 0L).takeIf { it > 0L }
            ?: return Result.failure()
        val item = repository.getDownload(id) ?: return Result.failure()
        // Ждём слот уже в foreground: иначе WorkManager остановит фоновую работу через 10 минут
        setForegroundIfAllowed(notificationService.createForegroundInfo(item, queued = true))
        return downloadSlots.withPermit {
            val foreground = setForegroundIfAllowed(notificationService.createForegroundInfo(item))
            if (!foreground) {
                analyticsTracker.logDownloadInfo {
                    "Download id=$id runs without foreground: system denied foreground start"
                }
            }
            downloadWithRetries(id, item)
        }
    }

    private suspend fun downloadWithRetries(id: Long, initialItem: VideoDownloadItem): Result {
        var item = initialItem
        // Полный внутренний диск на ТВ-боксе ломает не только загрузку, но и Room/DataStore приложения.
        if (applicationContext.filesDir.usableSpace < MIN_FREE_STORAGE_BYTES) {
            val message = applicationContext.getString(R.string.video_download_no_space)
            analyticsTracker.logDownloadInfo { "Download id=$id not started: free space below reserve" }
            repository.updateStatus(id = id, status = VideoDownloadStatus.Failed, errorMessage = message)
            analytics.reportFailed(item, message)
            return Result.failure()
        }
        var strategy = strategyResolver.resolve(item)
        val refreshBeforeStart = shouldRefreshBeforeStart(item)
        if (!refreshBeforeStart) {
            repository.updateStatus(
                id = id,
                status = VideoDownloadStatus.Downloading,
                errorMessage = null,
            )
        }
        if (refreshBeforeStart) {
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
                is DownloadStreamRefresh.Success -> {
                    item = result.item
                    strategy = strategyResolver.resolve(item)
                }

                is DownloadStreamRefresh.Failure -> return finishAfterRefreshFailure(
                    id = id,
                    item = item,
                    strategy = strategy,
                    reason = reason,
                    message = result.message,
                )
            }
        }

        var retriedAfterForbidden = false
        var transientRetryCount = 0
        while (true) {
            try {
                executor.download(
                    id = id,
                    item = item,
                    strategy = strategy,
                    retriedAfterForbidden = retriedAfterForbidden,
                    onProgressPercent = { percent -> publishProgress(item, percent) },
                )
                ensureDownloadIntegrity(id, item, strategy, retriedAfterForbidden)
                repository.updateStatus(
                    id = id,
                    status = VideoDownloadStatus.Downloaded,
                    progress = 1f,
                    errorMessage = null,
                )
                analytics.reportSucceeded(item)
                analyticsTracker.logDownloadInfo { "Completed download id=$id retryUsed=$retriedAfterForbidden" }
                exportRepository.enqueueAutoExportIfEnabled(id)
                return Result.success()
            } catch (throwable: Throwable) {
                currentCoroutineContext().ensureActive()
                if (isStopped) {
                    analyticsTracker.logDownloadInfo { "Stopped download id=$id retryUsed=$retriedAfterForbidden" }
                    return Result.failure()
                }
                val details = throwable.downloadFailureDetails()
                if (
                    !retriedAfterForbidden &&
                    throwable.isForbiddenHttpResponse()
                ) {
                    retriedAfterForbidden = true
                    analyticsTracker.logDownloadWarning(throwable) {
                        "Download id=$id got 403 from ${strategy.playerLabel}; " +
                                "refreshing iframe and stream before retry. " +
                                "details=$details"
                    }
                    when (
                        val refreshResult = refreshDownloadStream(
                            id = id,
                            item = item,
                            strategy = strategy,
                            reason = DownloadRestartReason.Forbidden,
                        )
                    ) {
                        is DownloadStreamRefresh.Success -> {
                            item = refreshResult.item
                            strategy = strategyResolver.resolve(item)
                            delay(TRANSIENT_RETRY_DELAY_MS)
                            continue
                        }

                        is DownloadStreamRefresh.Failure -> return finishAfterRefreshFailure(
                            id = id,
                            item = item,
                            strategy = strategy,
                            reason = DownloadRestartReason.Forbidden,
                            message = "$details; ${refreshResult.message}",
                            throwable = throwable,
                        )
                    }
                }

                if (
                    retriedAfterForbidden &&
                    throwable.isForbiddenHttpResponse() &&
                    runAttemptCount < MAX_STREAM_REFRESH_WORK_RETRIES
                ) {
                    val retryDetails = "$details; retryUsed=true; " +
                            "workRetry=${runAttemptCount.nextRetryAttempt()}/$MAX_STREAM_REFRESH_WORK_RETRIES"
                    analyticsTracker.logDownloadWarning(throwable) {
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
                    transientRetryCount < MAX_TRANSIENT_DOWNLOAD_RETRIES &&
                    throwable.isTransientDownloadFailure()
                ) {
                    transientRetryCount += 1
                    val nodeRefused = throwable.isOkCdnNodeRefusal()
                    analyticsTracker.logDownloadWarning(throwable) {
                        "Download id=$id got transient ${strategy.playerLabel} failure; " +
                                "refreshing iframe and stream before retry. " +
                                "attempt=$transientRetryCount/$MAX_TRANSIENT_DOWNLOAD_RETRIES " +
                                "details=$details"
                    }
                    when (
                        val refreshResult = refreshDownloadStream(
                            id = id,
                            item = item,
                            strategy = strategy,
                            reason = DownloadRestartReason.TransientFailure,
                            forceRefresh = nodeRefused,
                        )
                    ) {
                        is DownloadStreamRefresh.Success -> {
                            item = refreshResult.item
                            strategy = strategyResolver.resolve(item)
                            delay(TRANSIENT_RETRY_DELAY_MS)
                            continue
                        }

                        is DownloadStreamRefresh.Failure -> return finishAfterRefreshFailure(
                            id = id,
                            item = item,
                            strategy = strategy,
                            reason = DownloadRestartReason.TransientFailure,
                            message = "$details; ${refreshResult.message}",
                            throwable = throwable,
                        )
                    }
                }

                if (
                    throwable.isTransientDownloadFailure() &&
                    runAttemptCount < MAX_STREAM_REFRESH_WORK_RETRIES
                ) {
                    val retryDetails = "$details; localRetries=$transientRetryCount; " +
                            "workRetry=${runAttemptCount.nextRetryAttempt()}/" +
                            MAX_STREAM_REFRESH_WORK_RETRIES
                    analyticsTracker.logDownloadWarning(throwable) {
                        "Download id=$id exhausted local ${strategy.playerLabel} retries; scheduling worker retry. " +
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
                analyticsTracker.logDownloadWarning(throwable) {
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

    /**
     * Проверяет скачанный прогрессивный mp4 и перекачивает повреждённое. Докачка по кэшу не
     * заметила бы порчу: записанные байты считаются готовыми.
     *
     * Первый ремонт выбрасывает куски кэша вокруг места остановки экстрактора и докачивает только
     * их; второй — весь файл. Если и после этого файл не разбирается, источник отдаёт битый файл
     * сам, и загрузка падает с понятной причиной, а не отдаёт пользователю неиграющую серию.
     */
    private suspend fun ensureDownloadIntegrity(
        id: Long,
        item: VideoDownloadItem,
        strategy: DownloadPlayerStrategy,
        retriedAfterForbidden: Boolean,
    ) {
        if (item.streamUrl.streamKind() != StreamKind.Progressive) return
        var repairs = 0
        while (true) {
            when (val result = integrityVerifier.verify(item)) {
                DownloadIntegrityVerifier.Result.Valid -> {
                    if (repairs > 0) {
                        analyticsTracker.logDownloadInfo {
                            "Download id=$id passed integrity check after repairs=$repairs"
                        }
                    }
                    return
                }

                is DownloadIntegrityVerifier.Result.Skipped -> {
                    analyticsTracker.logDownloadInfo {
                        "Download id=$id integrity check skipped: ${result.reason}"
                    }
                    return
                }

                is DownloadIntegrityVerifier.Result.Corrupt -> {
                    analyticsTracker.logDownloadWarning(null) {
                        "Download id=$id is corrupt at byte=${result.position} " +
                            "detail=${result.detail} repairs=$repairs"
                    }
                    if (repairs >= MAX_INTEGRITY_REPAIRS) {
                        throw DownloadIntegrityException(
                            "Downloaded file is corrupt at byte ${result.position}: ${result.detail}",
                        )
                    }
                    val wholeFile = repairs > 0
                    repairs += 1
                    val evicted = integrityVerifier.evict(
                        item = item,
                        position = if (wholeFile) null else result.position,
                    )
                    analyticsTracker.logDownloadInfo {
                        "Download id=$id evicted bytes=$evicted wholeFile=$wholeFile; re-downloading"
                    }
                    executor.download(
                        id = id,
                        item = item,
                        strategy = strategy,
                        retriedAfterForbidden = retriedAfterForbidden,
                        onProgressPercent = { percent -> publishProgress(item, percent) },
                    )
                }
            }
        }
    }

    private fun shouldRefreshBeforeStart(item: VideoDownloadItem): Boolean =
        inputData.getBoolean(KEY_FORCE_STREAM_REFRESH, false) ||
                item.progress > 0f ||
                runAttemptCount > 0 ||
                item.errorMessage != null

    private suspend fun finishAfterRefreshFailure(
        id: Long,
        item: VideoDownloadItem,
        strategy: DownloadPlayerStrategy,
        reason: DownloadRestartReason,
        message: String,
        throwable: Throwable? = null,
    ): Result {
        val failedDetails = "refreshReason=$reason; refreshFailed=$message; " +
                "workRetry=${runAttemptCount.nextRetryAttempt()}/$MAX_STREAM_REFRESH_WORK_RETRIES"
        if (runAttemptCount < MAX_STREAM_REFRESH_WORK_RETRIES) {
            analyticsTracker.logDownloadWarning(throwable) {
                "Download id=$id will retry because fresh ${strategy.playerLabel} source is not ready. " +
                        "details=$failedDetails"
            }
            repository.updateStatus(
                id = id,
                status = VideoDownloadStatus.Queued,
                errorMessage = failedDetails,
            )
            return Result.retry()
        }
        analyticsTracker.logDownloadWarning(throwable) {
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

    private suspend fun refreshDownloadStream(
        id: Long,
        item: VideoDownloadItem,
        strategy: DownloadPlayerStrategy,
        reason: DownloadRestartReason,
        forceRefresh: Boolean = false,
    ): DownloadStreamRefresh {
        repository.updateStatus(
            id = id,
            status = VideoDownloadStatus.Resolving,
            errorMessage = null,
        )
        analyticsTracker.logDownloadInfo {
            "Refreshing download source id=$id reason=$reason " +
                    "workAttempt=${runAttemptCount + 1} player=${strategy.playerLabel} " +
                    "iframe=${item.iframeUrl.shortFingerprint()}"
        }
        return when (
            val refreshResult = streamRefresher.refresh(
                item = item,
                autoQualityLabel = DEFAULT_AUTO_QUALITY_LABEL,
                forceRefresh = forceRefresh,
            )
        ) {
            is VideoDownloadStreamRefreshResult.Success -> {
                val stream = refreshResult.stream
                // The adaptive manifest uses the stable custom key while its signed segment
                // URLs change. Remove only the manifest entry so the next downloader reads the
                // fresh playlist and still reuses already cached segments.
                strategy.manifestKeyToEvictOnRefresh(item)?.let { manifestKey ->
                    withContext(Dispatchers.IO) {
                        runCatching { cacheProvider.cache.removeResource(manifestKey) }
                    }
                }
                repository.updatePreparedStream(id, stream)
                repository.updateStatus(
                    id = id,
                    status = VideoDownloadStatus.Downloading,
                    errorMessage = null,
                )
                val refreshedItem = repository.getDownload(id) ?: item.copy(
                    videoId = stream.videoId,
                    playerName = stream.playerName,
                    playerId = stream.playerId,
                    dubbing = stream.dubbing,
                    iframeUrl = stream.iframeUrl,
                    qualityLabel = stream.qualityLabel,
                    streamUrl = stream.url,
                    headers = stream.headers,
                )
                val previousIframeFingerprint = item.iframeUrl.shortFingerprint()
                val refreshedIframeFingerprint = stream.iframeUrl.shortFingerprint()
                analyticsTracker.logDownloadInfo {
                    "Refreshed download source id=$id reason=$reason player=${stream.playerName} " +
                            "quality=${stream.qualityLabel} iframe=$previousIframeFingerprint->$refreshedIframeFingerprint " +
                            "changed=${previousIframeFingerprint != refreshedIframeFingerprint}"
                }
                DownloadStreamRefresh.Success(refreshedItem)
            }

            is VideoDownloadStreamRefreshResult.Failure ->
                DownloadStreamRefresh.Failure(refreshResult.message)
        }
    }

    private fun publishProgress(item: VideoDownloadItem, progressPercent: Int) {
        setForegroundAsync(notificationService.createForegroundInfo(item, progressPercent))
        setProgressAsync(workDataOf(KEY_PROGRESS to progressPercent))
    }

    private fun String.shortFingerprint(): String =
        MessageDigest.getInstance("SHA-256")
            .digest(toByteArray())
            .take(FINGERPRINT_BYTES)
            .joinToString("") { byte -> "%02x".format(byte.toInt() and 0xff) }

    /** Файл скачан целиком, но не разбирается как mp4 даже после перекачки. */
    private class DownloadIntegrityException(message: String) : java.io.IOException(message)

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
        const val KEY_FORCE_STREAM_REFRESH = "force_stream_refresh"
        private const val DEFAULT_AUTO_QUALITY_LABEL = "Auto"
        private const val MAX_STREAM_REFRESH_WORK_RETRIES = 3
        private const val MAX_TRANSIENT_DOWNLOAD_RETRIES = 3
        private const val MAX_INTEGRITY_REPAIRS = 2
        private const val TRANSIENT_RETRY_DELAY_MS = 3_000L
        private const val FINGERPRINT_BYTES = 4
        private const val MAX_PARALLEL_DOWNLOADS = 2

        // Запас, который не отдаём под загрузки: на нём живут БД и настройки приложения.
        private const val MIN_FREE_STORAGE_BYTES = 300L * 1024 * 1024

        // Общий на процесс: WorkManager запускает CoroutineWorker'ы без оглядки на свой executor,
        // и сезон целиком превращался бы в десяток параллельных загрузок с одного балансера
        private val downloadSlots = Semaphore(MAX_PARALLEL_DOWNLOADS)
    }
}
