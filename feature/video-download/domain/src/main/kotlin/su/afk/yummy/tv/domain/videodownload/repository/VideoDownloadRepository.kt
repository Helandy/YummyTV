package su.afk.yummy.tv.domain.videodownload.repository

import kotlinx.coroutines.flow.Flow
import su.afk.yummy.tv.domain.videodownload.model.VideoDownloadItem
import su.afk.yummy.tv.domain.videodownload.model.VideoDownloadRequest
import su.afk.yummy.tv.domain.videodownload.model.VideoDownloadRestartStream
import su.afk.yummy.tv.domain.videodownload.model.VideoDownloadStatus

interface VideoDownloadRepository {
    fun observeDownloads(): Flow<List<VideoDownloadItem>>
    fun observeStatuses(animeId: Int): Flow<Map<String, VideoDownloadItem>>
    suspend fun getDownload(id: Long): VideoDownloadItem?
    suspend fun enqueue(request: VideoDownloadRequest): VideoDownloadItem
    suspend fun pause(id: Long)
    suspend fun cancelOrDelete(id: Long)
    suspend fun restart(id: Long, stream: VideoDownloadRestartStream? = null)
    suspend fun updatePreparedStream(id: Long, stream: VideoDownloadRestartStream)
    suspend fun updateStatus(
        id: Long,
        status: VideoDownloadStatus,
        progress: Float? = null,
        bytesDownloaded: Long? = null,
        totalBytes: Long? = null,
        errorMessage: String? = null,
    )
}
