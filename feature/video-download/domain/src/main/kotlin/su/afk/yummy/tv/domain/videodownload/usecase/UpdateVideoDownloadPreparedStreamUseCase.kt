package su.afk.yummy.tv.domain.videodownload.usecase

import su.afk.yummy.tv.domain.videodownload.model.VideoDownloadRestartStream
import su.afk.yummy.tv.domain.videodownload.repository.VideoDownloadRepository
import javax.inject.Inject

/** Сохраняет подготовленные данные потока для существующей загрузки. */
class UpdateVideoDownloadPreparedStreamUseCase @Inject constructor(
    private val repository: VideoDownloadRepository,
) {
    suspend operator fun invoke(id: Long, stream: VideoDownloadRestartStream) {
        repository.updatePreparedStream(id, stream)
    }
}
