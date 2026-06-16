package su.afk.yummy.tv.domain.home.usecase

import su.afk.yummy.tv.domain.home.model.HomeFeed
import su.afk.yummy.tv.domain.home.repository.HomeFeedRepository
import javax.inject.Inject

/** Loads only the locally cached home feed without refreshing from the network. */
class GetCachedHomeFeedUseCase @Inject constructor(
    private val homeFeedRepository: HomeFeedRepository,
) {
    suspend operator fun invoke(): HomeFeed? = homeFeedRepository.getCachedHomeFeed()
}
