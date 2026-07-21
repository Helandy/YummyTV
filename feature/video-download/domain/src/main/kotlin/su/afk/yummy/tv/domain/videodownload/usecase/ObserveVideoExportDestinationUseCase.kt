package su.afk.yummy.tv.domain.videodownload.usecase

import kotlinx.coroutines.flow.Flow
import su.afk.yummy.tv.domain.videodownload.model.VideoExportDestination
import su.afk.yummy.tv.domain.videodownload.repository.VideoDownloadExportRepository
import javax.inject.Inject

/** Наблюдает за выбранной пользователем папкой экспорта. */
class ObserveVideoExportDestinationUseCase @Inject constructor(
    private val repository: VideoDownloadExportRepository,
) {
    operator fun invoke(): Flow<VideoExportDestination?> = repository.observeDestination()
}
