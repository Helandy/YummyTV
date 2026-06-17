package su.afk.yummy.tv.domain.account.usecase

import su.afk.yummy.tv.domain.account.model.VideoWatchSyncItem
import su.afk.yummy.tv.domain.account.repository.VideoWatchesRepository
import javax.inject.Inject

/** Отправляет локально сохранённые просмотры после входа пользователя. */
class SyncVideoWatchesUseCase @Inject constructor(
    private val repository: VideoWatchesRepository,
) {
    suspend operator fun invoke(videos: List<VideoWatchSyncItem>): Boolean =
        repository.syncWatched(videos)
}
