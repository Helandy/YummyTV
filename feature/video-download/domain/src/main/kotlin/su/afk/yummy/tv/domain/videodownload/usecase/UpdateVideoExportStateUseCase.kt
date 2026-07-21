package su.afk.yummy.tv.domain.videodownload.usecase

import su.afk.yummy.tv.domain.videodownload.model.VideoExportStatus
import su.afk.yummy.tv.domain.videodownload.repository.VideoDownloadExportRepository
import javax.inject.Inject

/** Обновляет состояние, прогресс и результат экспорта MP4. */
class UpdateVideoExportStateUseCase @Inject constructor(
    private val repository: VideoDownloadExportRepository,
) {
    suspend operator fun invoke(
        id: Long,
        status: VideoExportStatus,
        progress: Float? = null,
        destinationUri: String? = null,
        exportedFileUri: String? = null,
        errorMessage: String? = null,
    ) {
        repository.updateState(
            downloadId = id,
            status = status,
            progress = progress,
            destinationUri = destinationUri,
            exportedFileUri = exportedFileUri,
            errorMessage = errorMessage,
        )
    }
}
