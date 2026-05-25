package su.afk.yummy.tv.domain.account.usecase

import su.afk.yummy.tv.domain.account.model.*
import su.afk.yummy.tv.domain.account.repository.*

/** Toggles subscription state for updates on a video. */
class SetVideoSubscriptionUseCase(private val repository: VideoSubscriptionRepository) {
    suspend operator fun invoke(videoId: Int, subscribed: Boolean): Boolean =
        repository.setSubscribed(videoId, subscribed)
}
