package su.afk.yummy.tv.domain.videodownload.usecase

import su.afk.yummy.tv.domain.videodownload.repository.VideoDownloadExportRepository
import javax.inject.Inject

/** Отменяет экспорт выбранной скачанной серии. */
class CancelVideoExportUseCase @Inject constructor(
    private val repository: VideoDownloadExportRepository,
) {
    suspend operator fun invoke(id: Long) {
        repository.cancel(id)
    }
}
