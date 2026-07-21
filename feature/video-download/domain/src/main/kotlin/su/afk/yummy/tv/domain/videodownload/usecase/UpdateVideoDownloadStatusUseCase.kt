package su.afk.yummy.tv.domain.videodownload.usecase

import su.afk.yummy.tv.domain.videodownload.model.VideoDownloadStatus
import su.afk.yummy.tv.domain.videodownload.repository.VideoDownloadRepository
import javax.inject.Inject

/** Обновляет состояние и прогресс офлайн-загрузки. */
class UpdateVideoDownloadStatusUseCase @Inject constructor(
    private val repository: VideoDownloadRepository,
) {
    suspend operator fun invoke(
        id: Long,
        status: VideoDownloadStatus,
        progress: Float? = null,
        bytesDownloaded: Long? = null,
        totalBytes: Long? = null,
        errorMessage: String? = null,
    ) {
        repository.updateStatus(
            id = id,
            status = status,
            progress = progress,
            bytesDownloaded = bytesDownloaded,
            totalBytes = totalBytes,
            errorMessage = errorMessage,
        )
    }
}
