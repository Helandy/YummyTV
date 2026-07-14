package su.afk.yummy.tv.data.account.repository

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import su.afk.yummy.tv.core.preferences.settings.SettingsStore
import su.afk.yummy.tv.core.storage.account.AccountStorageStore
import su.afk.yummy.tv.core.storage.account.isFresh
import su.afk.yummy.tv.data.account.mapper.toVideoSubscription
import su.afk.yummy.tv.data.account.network.YaniAccountApi
import su.afk.yummy.tv.data.account.storage.mapper.toVideoSubscriptions
import su.afk.yummy.tv.data.account.storage.mapper.toVideoSubscriptionsCache
import su.afk.yummy.tv.domain.account.model.VideoSubscription
import su.afk.yummy.tv.domain.account.repository.VideoSubscriptionRepository

class YaniVideoSubscriptionRepository(
    private val api: YaniAccountApi,
    private val accountStorage: AccountStorageStore,
    private val settingsStore: SettingsStore,
) : VideoSubscriptionRepository {

    override suspend fun getSubscriptions(userId: Int): List<VideoSubscription> =
        withContext(Dispatchers.IO) {
            val language = settingsStore.yaniContentLanguage.first()
            val languageCode = language.apiCode
            val stored = accountStorage.getVideoSubscriptions(userId, languageCode)
            if (stored?.isFresh(ACCOUNT_SHORT_TTL_MS) == true) {
                return@withContext stored.toVideoSubscriptions()
            }

            try {
                fetchSubscriptions(userId, languageCode)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                stored?.toVideoSubscriptions()
                    ?: throw error
            }
        }

    override suspend fun setSubscribed(videoId: Int, subscribed: Boolean): Boolean =
        withContext(Dispatchers.IO) {
            val userId = settingsStore.yaniUserId.first()
            val result = if (subscribed) {
                api.setSubscribed(videoId)
            } else {
                api.removeSubscribed(videoId)
            }
            if (userId > 0) {
                accountStorage.deleteVideoSubscriptions(userId)
            }
            result
        }

    private suspend fun fetchSubscriptions(
        userId: Int,
        languageCode: String,
    ): List<VideoSubscription> {
        val subscriptions = api.getSubscriptions(userId).mapNotNull { it.toVideoSubscription() }
        val cache = subscriptions.toVideoSubscriptionsCache(
            userId = userId,
            language = languageCode,
            cachedAt = System.currentTimeMillis(),
        )
        accountStorage.saveVideoSubscriptions(cache)
        return cache.toVideoSubscriptions()
    }
}
