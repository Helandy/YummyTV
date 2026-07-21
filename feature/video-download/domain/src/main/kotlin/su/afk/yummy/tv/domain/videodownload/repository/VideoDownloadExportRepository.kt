package su.afk.yummy.tv.domain.videodownload.repository

import kotlinx.coroutines.flow.Flow
import su.afk.yummy.tv.domain.videodownload.model.VideoExportDestination
import su.afk.yummy.tv.domain.videodownload.model.VideoExportStatus

interface VideoDownloadExportRepository {
    fun observeDestination(): Flow<VideoExportDestination?>
    suspend fun selectDestination(uri: String): VideoExportDestination
    suspend fun enqueue(downloadIds: List<Long>, destination: VideoExportDestination)
    suspend fun cancel(downloadId: Long)
    suspend fun updateState(
        downloadId: Long,
        status: VideoExportStatus,
        progress: Float? = null,
        destinationUri: String? = null,
        exportedFileUri: String? = null,
        errorMessage: String? = null,
    )
}
