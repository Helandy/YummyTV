package su.afk.yummy.tv.data.account.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import su.afk.yummy.tv.data.account.mapper.toVideoSubscription
import su.afk.yummy.tv.data.account.network.YaniAccountApi
import su.afk.yummy.tv.domain.account.model.VideoSubscription
import su.afk.yummy.tv.domain.account.repository.VideoSubscriptionRepository

class YaniVideoSubscriptionRepository(
    private val api: YaniAccountApi,
) : VideoSubscriptionRepository {

    override suspend fun getSubscriptions(userId: Int): List<VideoSubscription> =
        withContext(Dispatchers.IO) { api.getSubscriptions(userId).mapNotNull { it.toVideoSubscription() } }

    override suspend fun setSubscribed(videoId: Int, subscribed: Boolean): Boolean =
        withContext(Dispatchers.IO) {
            if (subscribed) {
                api.setSubscribed(videoId)
            } else {
                api.removeSubscribed(videoId)
            }
        }
}
