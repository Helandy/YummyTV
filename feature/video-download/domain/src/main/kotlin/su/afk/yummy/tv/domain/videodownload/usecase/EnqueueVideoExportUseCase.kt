package su.afk.yummy.tv.domain.videodownload.usecase

import su.afk.yummy.tv.domain.videodownload.model.VideoExportDestination
import su.afk.yummy.tv.domain.videodownload.repository.VideoDownloadExportRepository
import javax.inject.Inject

/** Добавляет завершённые загрузки в очередь экспорта MP4. */
class EnqueueVideoExportUseCase @Inject constructor(
    private val repository: VideoDownloadExportRepository,
) {
    suspend operator fun invoke(ids: List<Long>, destination: VideoExportDestination) {
        repository.enqueue(ids, destination)
    }
}
