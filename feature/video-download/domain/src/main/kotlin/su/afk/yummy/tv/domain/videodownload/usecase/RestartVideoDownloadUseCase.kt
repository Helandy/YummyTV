package su.afk.yummy.tv.domain.videodownload.usecase

import su.afk.yummy.tv.domain.videodownload.model.VideoDownloadRestartStream
import su.afk.yummy.tv.domain.videodownload.repository.VideoDownloadRepository
import javax.inject.Inject

/** Перезапускает загрузку, при необходимости используя обновлённый поток. */
class RestartVideoDownloadUseCase @Inject constructor(
    private val repository: VideoDownloadRepository,
) {
    suspend operator fun invoke(id: Long, stream: VideoDownloadRestartStream? = null) {
        repository.restart(id, stream)
    }
}
