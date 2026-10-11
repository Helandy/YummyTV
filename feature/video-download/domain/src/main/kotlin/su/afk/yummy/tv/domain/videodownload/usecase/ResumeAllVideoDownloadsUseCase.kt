package su.afk.yummy.tv.domain.videodownload.usecase

import su.afk.yummy.tv.domain.videodownload.repository.VideoDownloadRepository
import javax.inject.Inject

/**
 * Возобновляет все загрузки, поставленные на паузу, возвращая их в очередь. Запуск идёт в общем
 * порядке очереди с её лимитом параллельных загрузок и сетевой политикой.
 */
class ResumeAllVideoDownloadsUseCase @Inject constructor(
    private val repository: VideoDownloadRepository,
) {
    suspend operator fun invoke() {
        repository.resumeAll()
    }
}
