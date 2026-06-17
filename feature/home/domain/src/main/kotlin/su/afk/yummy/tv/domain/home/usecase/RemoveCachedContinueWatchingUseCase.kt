package su.afk.yummy.tv.domain.home.usecase

import su.afk.yummy.tv.domain.home.repository.HomeFeedRepository
import javax.inject.Inject

/** Скрывает тайтл из кешированного и локального продолжения просмотра. */
class RemoveCachedContinueWatchingUseCase @Inject constructor(
    private val homeFeedRepository: HomeFeedRepository,
) {
    suspend operator fun invoke(animeId: Int) {
        homeFeedRepository.removeCachedContinueWatching(animeId)
    }
}
