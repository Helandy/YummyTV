package su.afk.yummy.tv.domain.account.usecase

import su.afk.yummy.tv.domain.account.model.VideoSubscription
import su.afk.yummy.tv.domain.account.repository.VideoSubscriptionRepository

/** Loads user's anime dubbing/player subscriptions. */
class GetVideoSubscriptionsUseCase(private val repository: VideoSubscriptionRepository) {
    suspend operator fun invoke(userId: Int): List<VideoSubscription> =
        repository.getSubscriptions(userId)
}
