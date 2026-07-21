package su.afk.yummy.tv.domain.videodownload.usecase

import su.afk.yummy.tv.domain.videodownload.model.VideoExportDestination
import su.afk.yummy.tv.domain.videodownload.repository.VideoDownloadExportRepository
import javax.inject.Inject

/** Сохраняет выбранную пользователем папку экспорта. */
class SelectVideoExportDestinationUseCase @Inject constructor(
    private val repository: VideoDownloadExportRepository,
) {
    suspend operator fun invoke(uri: String): VideoExportDestination =
        repository.selectDestination(uri)
}
