package su.afk.yummy.tv.domain.videodownload.repository

import kotlinx.coroutines.flow.Flow
import su.afk.yummy.tv.domain.videodownload.model.VideoExportDestination
import su.afk.yummy.tv.domain.videodownload.model.VideoExportSource
import su.afk.yummy.tv.domain.videodownload.model.VideoExportStatus

interface VideoDownloadExportRepository {
    fun observeDestination(): Flow<VideoExportDestination?>
    suspend fun selectDestination(uri: String): VideoExportDestination
    suspend fun isDestinationWritable(uri: String): Boolean

    /** Лежит ли ранее экспортированный файл на месте: его могли удалить руками. */
    suspend fun exportedFileExists(uri: String): Boolean
    suspend fun enqueue(
        downloadIds: List<Long>,
        destination: VideoExportDestination,
        source: VideoExportSource = VideoExportSource.Manual,
    )

    /** Ставит экспорт после успешного скачивания, если автоэкспорт включён и папка доступна. */
    suspend fun enqueueAutoExportIfEnabled(downloadId: Long)
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
