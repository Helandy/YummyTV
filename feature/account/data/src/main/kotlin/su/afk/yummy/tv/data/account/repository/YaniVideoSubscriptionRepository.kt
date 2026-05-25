package su.afk.yummy.tv.data.account.repository

import su.afk.yummy.tv.data.account.network.YaniAccountApi
import su.afk.yummy.tv.domain.account.repository.VideoSubscriptionRepository

class YaniVideoSubscriptionRepository(
    private val api: YaniAccountApi,
) : VideoSubscriptionRepository {

    override suspend fun setSubscribed(videoId: Int, subscribed: Boolean): Boolean =
        if (subscribed) {
            api.setSubscribed(videoId)
        } else {
            api.removeSubscribed(videoId)
        }
}
