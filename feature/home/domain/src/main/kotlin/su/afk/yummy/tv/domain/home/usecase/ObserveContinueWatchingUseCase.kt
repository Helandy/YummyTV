package su.afk.yummy.tv.domain.home.usecase

import kotlinx.coroutines.flow.Flow
import su.afk.yummy.tv.domain.home.model.HomeContinueWatchingItem
import su.afk.yummy.tv.domain.home.repository.HomeFeedRepository
import javax.inject.Inject

/** Observes merged remote feed and local playback progress for continue watching. */
class ObserveContinueWatchingUseCase @Inject constructor(
    private val homeFeedRepository: HomeFeedRepository,
) {
    operator fun invoke(): Flow<List<HomeContinueWatchingItem>> =
        homeFeedRepository.observeContinueWatching()
}
