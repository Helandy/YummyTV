package su.afk.yummy.tv.domain.home.usecase

import kotlinx.coroutines.flow.Flow
import su.afk.yummy.tv.domain.home.model.HomeContinueWatchingItem
import su.afk.yummy.tv.domain.home.repository.HomeFeedRepository
import javax.inject.Inject

/** Наблюдает за объединённым списком продолжения просмотра из ленты и локального прогресса. */
class ObserveContinueWatchingUseCase @Inject constructor(
    private val homeFeedRepository: HomeFeedRepository,
) {
    operator fun invoke(): Flow<List<HomeContinueWatchingItem>> =
        homeFeedRepository.observeContinueWatching()
}
