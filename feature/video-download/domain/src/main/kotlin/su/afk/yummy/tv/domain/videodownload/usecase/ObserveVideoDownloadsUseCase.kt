package su.afk.yummy.tv.domain.videodownload.usecase

import kotlinx.coroutines.flow.Flow
import su.afk.yummy.tv.domain.videodownload.model.VideoDownloadItem
import su.afk.yummy.tv.domain.videodownload.repository.VideoDownloadRepository
import javax.inject.Inject

/** Наблюдает за всеми сохранёнными и активными загрузками. */
class ObserveVideoDownloadsUseCase @Inject constructor(
    private val repository: VideoDownloadRepository,
) {
    operator fun invoke(): Flow<List<VideoDownloadItem>> = repository.observeDownloads()
}
