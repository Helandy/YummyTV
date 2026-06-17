package su.afk.yummy.tv.domain.home.usecase

import su.afk.yummy.tv.domain.home.repository.HomeFeedRepository
import javax.inject.Inject

/** Returns known playable video ids for a continue-watching title. */
class GetContinueWatchingVideoIdsUseCase @Inject constructor(
    private val homeFeedRepository: HomeFeedRepository,
) {
    suspend operator fun invoke(animeId: Int): List<Int> =
        homeFeedRepository.getContinueWatchingVideoIds(animeId)
}
