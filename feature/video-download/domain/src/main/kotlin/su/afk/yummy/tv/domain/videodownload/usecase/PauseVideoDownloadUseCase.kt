package su.afk.yummy.tv.domain.videodownload.usecase

import su.afk.yummy.tv.domain.videodownload.repository.VideoDownloadRepository
import javax.inject.Inject

/** Приостанавливает активную офлайн-загрузку серии. */
class PauseVideoDownloadUseCase @Inject constructor(
    private val repository: VideoDownloadRepository,
) {
    suspend operator fun invoke(id: Long) {
        repository.pause(id)
    }
}
