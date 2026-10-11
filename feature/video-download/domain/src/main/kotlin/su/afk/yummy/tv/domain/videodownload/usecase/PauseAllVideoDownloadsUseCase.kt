package su.afk.yummy.tv.domain.videodownload.usecase

import su.afk.yummy.tv.domain.videodownload.repository.VideoDownloadRepository
import javax.inject.Inject

/**
 * Приостанавливает все активные и ожидающие в очереди офлайн-загрузки. Уже скачанные, упавшие и
 * поставленные на паузу записи не затрагиваются; докачка продолжится с сохранённого места.
 */
class PauseAllVideoDownloadsUseCase @Inject constructor(
    private val repository: VideoDownloadRepository,
) {
    suspend operator fun invoke() {
        repository.pauseAll()
    }
}
