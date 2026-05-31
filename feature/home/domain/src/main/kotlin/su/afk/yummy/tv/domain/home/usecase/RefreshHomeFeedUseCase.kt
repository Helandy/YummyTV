package su.afk.yummy.tv.domain.home.usecase

import su.afk.yummy.tv.domain.home.model.HomeFeed
import su.afk.yummy.tv.domain.home.repository.HomeFeedRepository

/** Refreshes the cached home feed from the network. */
class RefreshHomeFeedUseCase(
    private val homeFeedRepository: HomeFeedRepository,
) {
    suspend operator fun invoke(): HomeFeed = homeFeedRepository.refreshHomeFeed()
}
