package su.afk.yummy.tv.domain.account

interface VideoSubscriptionRepository {
    suspend fun setSubscribed(videoId: Int, subscribed: Boolean): Boolean
}
