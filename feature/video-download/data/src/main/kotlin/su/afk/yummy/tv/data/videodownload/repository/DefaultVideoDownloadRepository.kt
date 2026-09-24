package su.afk.yummy.tv.data.videodownload.repository

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.work.BackoffPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.await
import androidx.work.workDataOf
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import su.afk.yummy.tv.core.storage.videodownload.VideoDownloadStorage
import su.afk.yummy.tv.core.utils.coroutines.runSuspendCatching
import su.afk.yummy.tv.data.videodownload.R
import su.afk.yummy.tv.data.videodownload.cache.VideoDownloadCacheProvider
import su.afk.yummy.tv.data.videodownload.cache.downloadResourcePrefixes
import su.afk.yummy.tv.data.videodownload.mapper.storageName
import su.afk.yummy.tv.data.videodownload.mapper.toDomain
import su.afk.yummy.tv.data.videodownload.mapper.toEntry
import su.afk.yummy.tv.data.videodownload.mapper.toVideoDownloadHeadersJson
import su.afk.yummy.tv.data.videodownload.notification.VideoDownloadNotificationService
import su.afk.yummy.tv.data.videodownload.strategy.DownloadPlayerStrategyResolver
import su.afk.yummy.tv.data.videodownload.worker.VideoDownloadAnalytics
import su.afk.yummy.tv.data.videodownload.worker.VideoDownloadWorker
import su.afk.yummy.tv.domain.videodownload.model.VideoDownloadItem
import su.afk.yummy.tv.domain.videodownload.model.VideoDownloadRequest
import su.afk.yummy.tv.domain.videodownload.model.VideoDownloadRestartStream
import su.afk.yummy.tv.domain.videodownload.model.VideoDownloadStatus
import su.afk.yummy.tv.domain.videodownload.repository.VideoDownloadRepository
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@OptIn(UnstableApi::class)
class DefaultVideoDownloadRepository @Inject internal constructor(
    @ApplicationContext private val context: Context,
    private val store: VideoDownloadStorage,
    private val cacheProvider: VideoDownloadCacheProvider,
    private val notificationService: VideoDownloadNotificationService,
    private val analytics: VideoDownloadAnalytics,
    private val strategyResolver: DownloadPlayerStrategyResolver,
) : VideoDownloadRepository {
    private val orphanReconciliationMutex = Mutex()
    private val enqueueMutex = Mutex()

    override fun observeDownloads(): Flow<List<VideoDownloadItem>> =
        store.observeDownloads()
            .onStart { reconcileOrphanedDownloads() }
            .map { entries ->
                entries.map { it.toDomain() }.visibleAfterLatestEpisodeDeletion()
            }
            .distinctUntilChanged()

    override fun observeStatuses(animeId: Int): Flow<Map<String, VideoDownloadItem>> =
        store.observeDownloadsForAnime(animeId)
            .onStart { reconcileOrphanedDownloads() }
            .map { entries ->
                entries.map { it.toDomain() }
                    .visibleAfterLatestEpisodeDeletion()
                    .groupBy { it.statusKey }
                    .mapValues { (_, group) -> group.maxBy { it.createdAt } }
            }
            .distinctUntilChanged()

    override suspend fun getDownload(id: Long): VideoDownloadItem? =
        store.getById(id)?.toDomain()

    override suspend fun enqueue(request: VideoDownloadRequest): VideoDownloadItem =
        enqueueMutex.withLock { enqueueLocked(request) }

    private suspend fun enqueueLocked(request: VideoDownloadRequest): VideoDownloadItem {
        val duplicate = store.findEpisodeDownload(
            animeId = request.animeId,
            episode = request.episode,
        )
        val now = System.currentTimeMillis()
        if (duplicate != null) {
            val duplicateStatus = duplicate.status.toVideoDownloadStatus()
            if (
                duplicateStatus == VideoDownloadStatus.Failed ||
                duplicateStatus == VideoDownloadStatus.Deleted
            ) {
                store.update(
                    request.toEntry(now, request.cacheKeyScheme()).copy(
                        id = duplicate.id,
                        cacheKey = duplicate.cacheKey,
                        progress = duplicate.progress,
                        bytesDownloaded = duplicate.bytesDownloaded,
                        totalBytes = duplicate.totalBytes,
                        createdAt = duplicate.createdAt,
                    )
                )
                scheduleWorker(
                    id = duplicate.id,
                    policy = ExistingWorkPolicy.REPLACE,
                    forceStreamRefresh = true,
                )
                return (
                        store.getById(duplicate.id)?.toDomain()
                            ?: error("Restarted video download is missing: ${duplicate.id}")
                        ).also { analytics.reportEnqueued(it, restarted = true) }
            }
            return duplicate.toDomain()
        }

        val id = store.insert(request.toEntry(now, request.cacheKeyScheme()))
        scheduleWorker(id)
        return (
                store.getById(id)?.toDomain()
                    ?: error("Inserted video download is missing: $id")
                ).also { analytics.reportEnqueued(it, restarted = false) }
    }

    override suspend fun pause(id: Long) {
        WorkManager.getInstance(context)
            .cancelUniqueWork(uniqueWorkName(id))
            .await()
        val entry = store.getById(id) ?: return
        val status = entry.status.toVideoDownloadStatus()
        if (status != VideoDownloadStatus.Queued && status != VideoDownloadStatus.Downloading) {
            return
        }
        store.update(
            entry.copy(
                status = VideoDownloadStatus.Paused.storageName(),
                errorMessage = null,
                updatedAt = System.currentTimeMillis(),
            )
        )
        store.getById(id)?.toDomain()?.let(notificationService::showPaused)
    }

    override suspend fun cancelOrDelete(id: Long) {
        val entry = store.getById(id) ?: return
        val episodeDownloads = store.getEpisodeDownloads(
            animeId = entry.animeId,
            episode = entry.episode,
        )
        val workManager = WorkManager.getInstance(context)
        episodeDownloads.forEach { download ->
            workManager.cancelUniqueWork(uniqueWorkName(download.id)).await()
            workManager.cancelUniqueWork(
                DefaultVideoDownloadExportRepository.uniqueWorkName(download.id)
            ).await()
        }
        episodeDownloads.map { it.cacheKey }.distinct().forEach { cacheKey ->
            runCatching { cacheProvider.cache.removeResource(cacheKey) }
            val resourcePrefixes = downloadResourcePrefixes(cacheKey)
            cacheProvider.cache.keys
                .filter { key -> resourcePrefixes.any(key::startsWith) }
                .forEach { key -> runCatching { cacheProvider.cache.removeResource(key) } }
        }
        store.markEpisodeDeleted(
            animeId = entry.animeId,
            episode = entry.episode,
            updatedAt = System.currentTimeMillis(),
        )
        episodeDownloads.forEach { download -> notificationService.cancel(download.id) }
        // Никакой глобальной чистки кэша: ключи загрузок схемы Legacy лежат под сырыми URL и
        // неотличимы от чужих, поэтому такой проход сносил данные всех остальных серий.
        // Осиротевшее подберёт LegacyStreamingCachePruner на старте, когда Legacy не останется.
    }

    override suspend fun restart(id: Long, stream: VideoDownloadRestartStream?) {
        val workManager = WorkManager.getInstance(context)
        workManager.cancelUniqueWork(uniqueWorkName(id))
        val entry = store.getById(id) ?: return
        val qualityLabel = stream?.qualityLabel ?: entry.qualityLabel
        // cacheKeyScheme сознательно не пересчитывается: сюда же приходит возобновление с паузы,
        // а смена схемы обесценила бы уже скачанные сегменты. Схема фиксируется только в enqueue.
        store.update(
            entry.copy(
                videoId = stream?.videoId ?: entry.videoId,
                playerName = stream?.playerName ?: entry.playerName,
                playerId = if (stream != null) stream.playerId else entry.playerId,
                dubbing = stream?.dubbing ?: entry.dubbing,
                iframeUrl = stream?.iframeUrl ?: entry.iframeUrl,
                qualityLabel = qualityLabel,
                streamUrl = stream?.url ?: entry.streamUrl,
                headersJson = stream?.headers?.toVideoDownloadHeadersJson() ?: entry.headersJson,
                status = VideoDownloadStatus.Queued.storageName(),
                errorMessage = null,
                updatedAt = System.currentTimeMillis(),
            )
        )
        notificationService.cancel(id)
        scheduleWorker(
            id = id,
            policy = ExistingWorkPolicy.REPLACE,
            forceStreamRefresh = true,
        )
    }

    override suspend fun updatePreparedStream(id: Long, stream: VideoDownloadRestartStream) {
        val entry = store.getById(id) ?: return
        store.update(
            entry.copy(
                videoId = stream.videoId,
                playerName = stream.playerName,
                playerId = stream.playerId,
                dubbing = stream.dubbing,
                iframeUrl = stream.iframeUrl,
                qualityLabel = stream.qualityLabel,
                streamUrl = stream.url,
                headersJson = stream.headers.toVideoDownloadHeadersJson(),
                errorMessage = null,
                updatedAt = System.currentTimeMillis(),
            )
        )
    }

    override suspend fun updateStatus(
        id: Long,
        status: VideoDownloadStatus,
        progress: Float?,
        bytesDownloaded: Long?,
        totalBytes: Long?,
        errorMessage: String?,
    ) {
        val entry = store.getById(id) ?: return
        val currentStatus = entry.status.toVideoDownloadStatus()
        if (currentStatus == VideoDownloadStatus.Deleted || currentStatus == VideoDownloadStatus.Paused) {
            return
        }
        store.update(
            entry.copy(
                status = status.storageName(),
                progress = progress ?: entry.progress,
                bytesDownloaded = bytesDownloaded ?: entry.bytesDownloaded,
                totalBytes = totalBytes ?: entry.totalBytes,
                errorMessage = errorMessage,
                updatedAt = System.currentTimeMillis(),
            )
        )
        if (status == VideoDownloadStatus.Downloaded) {
            store.markOtherFailedEpisodeDownloadsDeleted(
                animeId = entry.animeId,
                episode = entry.episode,
                keepId = id,
                updatedAt = System.currentTimeMillis(),
            )
        }
    }

    private fun VideoDownloadRequest.cacheKeyScheme() = strategyResolver.cacheKeyScheme(
        iframeUrl = iframeUrl,
        playerName = playerName,
        streamUrl = quality.url,
    )

    private fun scheduleWorker(
        id: Long,
        policy: ExistingWorkPolicy = ExistingWorkPolicy.KEEP,
        forceStreamRefresh: Boolean = false,
    ) {
        val request = OneTimeWorkRequestBuilder<VideoDownloadWorker>()
            .setInputData(
                workDataOf(
                    VideoDownloadWorker.KEY_DOWNLOAD_ID to id,
                    VideoDownloadWorker.KEY_FORCE_STREAM_REFRESH to forceStreamRefresh,
                )
            )
            .setBackoffCriteria(
                BackoffPolicy.LINEAR,
                DOWNLOAD_RETRY_BACKOFF_MS,
                TimeUnit.MILLISECONDS,
            )
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            uniqueWorkName(id),
            policy,
            request,
        )
    }

    private suspend fun reconcileOrphanedDownloads() = orphanReconciliationMutex.withLock {
        val workManager = WorkManager.getInstance(context)
        val now = System.currentTimeMillis()
        store.getUnfinishedDownloads()
            .filter { now - it.updatedAt >= ORPHAN_GRACE_PERIOD_MS }
            .forEach { entry ->
                val workInfos = runSuspendCatching {
                    workManager.getWorkInfosForUniqueWorkFlow(uniqueWorkName(entry.id)).first()
                }.getOrNull() ?: return@forEach
                val hasActiveWork = workInfos.any { workInfo ->
                    workInfo.state == WorkInfo.State.ENQUEUED ||
                            workInfo.state == WorkInfo.State.RUNNING ||
                            workInfo.state == WorkInfo.State.BLOCKED
                }
                if (hasActiveWork) return@forEach

                val latestEntry = store.getById(entry.id) ?: return@forEach
                if (latestEntry.status !in UNFINISHED_STORAGE_STATUSES) return@forEach
                store.update(
                    latestEntry.copy(
                        status = VideoDownloadStatus.Failed.storageName(),
                        errorMessage = context.getString(R.string.video_download_worker_stopped),
                        updatedAt = System.currentTimeMillis(),
                    )
                )
                notificationService.cancel(entry.id)
            }
    }

    companion object {
        private const val DOWNLOAD_RETRY_BACKOFF_MS = 10_000L
        private const val ORPHAN_GRACE_PERIOD_MS = 60_000L
        private val UNFINISHED_STORAGE_STATUSES = setOf(
            VideoDownloadStatus.Resolving.storageName(),
            VideoDownloadStatus.Queued.storageName(),
            VideoDownloadStatus.Downloading.storageName(),
        )

        fun uniqueWorkName(id: Long): String = "video_download_$id"
    }
}

private val VideoDownloadItem.statusKey: String
    get() = listOf(animeId.toString(), videoId.toString(), iframeUrl).joinToString("|")

private fun List<VideoDownloadItem>.visibleAfterLatestEpisodeDeletion(): List<VideoDownloadItem> =
    groupBy { it.animeId to it.episode }
        .values
        .flatMap { episodeDownloads ->
            val deletedAt = episodeDownloads
                .filter { it.status == VideoDownloadStatus.Deleted }
                .maxOfOrNull { it.updatedAt }
            episodeDownloads.filter { item ->
                item.status != VideoDownloadStatus.Deleted &&
                        (deletedAt == null || item.updatedAt > deletedAt)
            }
        }
        // порядок задаёт момент добавления: прогресс параллельных загрузок не должен тасовать список
        .sortedWith(
            compareByDescending<VideoDownloadItem> { it.createdAt }.thenByDescending { it.id },
        )

private fun String.toVideoDownloadStatus(): VideoDownloadStatus =
    runCatching { VideoDownloadStatus.valueOf(this) }.getOrDefault(VideoDownloadStatus.Failed)
