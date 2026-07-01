package su.afk.yummy.tv.data.videodownload.repository

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import su.afk.yummy.tv.core.storage.videodownload.VideoDownloadStore
import su.afk.yummy.tv.data.videodownload.cache.VideoDownloadCacheProvider
import su.afk.yummy.tv.data.videodownload.worker.VideoDownloadWorker
import su.afk.yummy.tv.domain.videodownload.model.VideoDownloadItem
import su.afk.yummy.tv.domain.videodownload.model.VideoDownloadRequest
import su.afk.yummy.tv.domain.videodownload.model.VideoDownloadStatus
import su.afk.yummy.tv.domain.videodownload.repository.VideoDownloadRepository
import javax.inject.Inject

@OptIn(UnstableApi::class)
class DefaultVideoDownloadRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val store: VideoDownloadStore,
    private val cacheProvider: VideoDownloadCacheProvider,
) : VideoDownloadRepository {
    override fun observeDownloads(): Flow<List<VideoDownloadItem>> =
        store.dao.observeDownloads().map { entries -> entries.map { it.toDomain() } }

    override fun observeStatuses(animeId: Int): Flow<Map<String, VideoDownloadItem>> =
        store.dao.observeDownloadsForAnime(animeId).map { entries ->
            entries
                .map { it.toDomain() }
                .associateBy { it.statusKey }
        }

    override suspend fun getDownload(id: Long): VideoDownloadItem? =
        store.dao.getById(id)?.toDomain()

    override suspend fun enqueue(request: VideoDownloadRequest): VideoDownloadItem {
        val duplicate = store.dao.findActiveDuplicate(
            animeId = request.animeId,
            videoId = request.videoId,
            iframeUrl = request.iframeUrl,
            qualityLabel = request.quality.label,
        )
        if (duplicate != null) return duplicate.toDomain()

        val now = System.currentTimeMillis()
        val id = store.dao.insert(request.toEntry(now))
        scheduleWorker(id)
        return store.dao.getById(id)?.toDomain()
            ?: error("Inserted video download is missing: $id")
    }

    override suspend fun cancelOrDelete(id: Long) {
        WorkManager.getInstance(context).cancelUniqueWork(uniqueWorkName(id))
        val entry = store.dao.getById(id) ?: return
        runCatching { cacheProvider.cache.removeResource(entry.cacheKey) }
        store.dao.update(
            entry.copy(
                status = VideoDownloadStatus.Deleted.storageName(),
                progress = 0f,
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
        val entry = store.dao.getById(id) ?: return
        store.dao.update(
            entry.copy(
                status = status.storageName(),
                progress = progress ?: entry.progress,
                bytesDownloaded = bytesDownloaded ?: entry.bytesDownloaded,
                totalBytes = totalBytes ?: entry.totalBytes,
                errorMessage = errorMessage,
                updatedAt = System.currentTimeMillis(),
            )
        )
    }

    private fun scheduleWorker(id: Long) {
        val request = OneTimeWorkRequestBuilder<VideoDownloadWorker>()
            .setInputData(workDataOf(VideoDownloadWorker.KEY_DOWNLOAD_ID to id))
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            uniqueWorkName(id),
            ExistingWorkPolicy.KEEP,
            request,
        )
    }

    companion object {
        fun uniqueWorkName(id: Long): String = "video_download_$id"
    }
}

private val VideoDownloadItem.statusKey: String
    get() = listOf(animeId.toString(), videoId.toString(), iframeUrl).joinToString("|")
