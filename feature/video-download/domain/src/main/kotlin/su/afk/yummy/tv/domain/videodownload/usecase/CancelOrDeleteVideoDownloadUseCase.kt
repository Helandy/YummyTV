package su.afk.yummy.tv.domain.videodownload.usecase

import su.afk.yummy.tv.domain.videodownload.repository.VideoDownloadRepository
import javax.inject.Inject

/** Отменяет активную загрузку либо удаляет сохранённую серию. */
class CancelOrDeleteVideoDownloadUseCase @Inject constructor(
    private val repository: VideoDownloadRepository,
) {
    suspend operator fun invoke(id: Long) {
        repository.cancelOrDelete(id)
    }
}
