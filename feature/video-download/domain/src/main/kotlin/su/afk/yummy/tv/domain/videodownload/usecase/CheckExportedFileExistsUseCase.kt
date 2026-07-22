package su.afk.yummy.tv.domain.videodownload.usecase

import su.afk.yummy.tv.domain.videodownload.repository.VideoDownloadExportRepository
import javax.inject.Inject

/** Проверяет, лежит ли ранее экспортированный файл в папке экспорта. */
class CheckExportedFileExistsUseCase @Inject constructor(
    private val repository: VideoDownloadExportRepository,
) {
    suspend operator fun invoke(uri: String?): Boolean =
        repository.exportedFileExists(uri.orEmpty())
}
