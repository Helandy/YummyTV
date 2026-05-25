package su.afk.yummy.tv.domain.account.repository

import su.afk.yummy.tv.domain.account.model.*

interface VideoSubscriptionRepository {
    suspend fun setSubscribed(videoId: Int, subscribed: Boolean): Boolean
}
