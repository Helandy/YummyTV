package su.afk.yummy.tv.data.account.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import su.afk.yummy.tv.core.preferences.settings.YaniAccountSettingsStore
import su.afk.yummy.tv.core.preferences.settings.currentLanguageCode
import su.afk.yummy.tv.core.storage.account.AccountStorage
import su.afk.yummy.tv.core.storage.account.AccountVideoSubscriptionsCache
import su.afk.yummy.tv.core.storage.account.isFresh
import su.afk.yummy.tv.core.storage.offlinefirst.offlineFirstCache
import su.afk.yummy.tv.data.account.network.YaniAccountApi
import su.afk.yummy.tv.data.account.storage.mapper.toVideoSubscriptions
import su.afk.yummy.tv.data.account.storage.mapper.toVideoSubscriptionsCache
import su.afk.yummy.tv.domain.account.model.VideoSubscription
import su.afk.yummy.tv.domain.account.repository.VideoSubscriptionRepository

class YaniVideoSubscriptionRepository(
    private val api: YaniAccountApi,
    private val accountStorage: AccountStorage,
    private val settingsStore: YaniAccountSettingsStore,
) : VideoSubscriptionRepository {

    override suspend fun getSubscriptions(userId: Int): List<VideoSubscription> =
        withContext(Dispatchers.IO) {
            val languageCode = settingsStore.currentLanguageCode()
            offlineFirstCache(
                read = { accountStorage.getVideoSubscriptions(userId, languageCode) },
                isFresh = { it.isFresh(ACCOUNT_SHORT_TTL_MS) },
                toDomain = { it.toVideoSubscriptions() },
                fetchAndSave = { fetchSubscriptions(userId, languageCode) },
            )
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
    ): AccountVideoSubscriptionsCache {
        val cache = api.getSubscriptions(userId).toVideoSubscriptionsCache(
            userId = userId,
            language = languageCode,
            cachedAt = System.currentTimeMillis(),
        )
        accountStorage.saveVideoSubscriptions(cache)
        return cache
    }
}
