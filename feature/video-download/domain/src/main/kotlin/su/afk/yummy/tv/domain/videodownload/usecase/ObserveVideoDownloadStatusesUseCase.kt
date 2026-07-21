package su.afk.yummy.tv.domain.videodownload.usecase

import kotlinx.coroutines.flow.Flow
import su.afk.yummy.tv.domain.videodownload.model.VideoDownloadItem
import su.afk.yummy.tv.domain.videodownload.repository.VideoDownloadRepository
import javax.inject.Inject

/** Наблюдает за состояниями загрузок серий выбранного аниме. */
class ObserveVideoDownloadStatusesUseCase @Inject constructor(
    private val repository: VideoDownloadRepository,
) {
    operator fun invoke(animeId: Int): Flow<Map<String, VideoDownloadItem>> =
        repository.observeStatuses(animeId)
}
