package su.afk.yummy.tv.domain.account.repository

import su.afk.yummy.tv.domain.account.model.VideoSubscription

interface VideoSubscriptionRepository {
    suspend fun getSubscriptions(userId: Int): List<VideoSubscription>
    suspend fun setSubscribed(videoId: Int, subscribed: Boolean): Boolean
}
