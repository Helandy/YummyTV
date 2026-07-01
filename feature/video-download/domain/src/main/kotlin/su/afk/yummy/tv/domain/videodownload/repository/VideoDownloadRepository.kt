package su.afk.yummy.tv.domain.videodownload.repository

import kotlinx.coroutines.flow.Flow
import su.afk.yummy.tv.domain.videodownload.model.VideoDownloadItem
import su.afk.yummy.tv.domain.videodownload.model.VideoDownloadRequest
import su.afk.yummy.tv.domain.videodownload.model.VideoDownloadStatus

interface VideoDownloadRepository {
    fun observeDownloads(): Flow<List<VideoDownloadItem>>
    fun observeStatuses(animeId: Int): Flow<Map<String, VideoDownloadItem>>
    suspend fun getDownload(id: Long): VideoDownloadItem?
    suspend fun enqueue(request: VideoDownloadRequest): VideoDownloadItem
    suspend fun cancelOrDelete(id: Long)
    suspend fun updateStatus(
        id: Long,
        status: VideoDownloadStatus,
        progress: Float? = null,
        bytesDownloaded: Long? = null,
        totalBytes: Long? = null,
        errorMessage: String? = null,
    )
}
