package su.afk.yummy.tv.data.account.repository

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import su.afk.yummy.tv.core.preferences.settings.SettingsStore
import su.afk.yummy.tv.core.preferences.settings.YaniContentLanguage
import su.afk.yummy.tv.core.storage.account.AccountStorageStore
import su.afk.yummy.tv.core.storage.account.isFresh
import su.afk.yummy.tv.core.storage.cache.CacheStore
import su.afk.yummy.tv.data.account.dto.YaniVideoSubscriptionsResponseDto
import su.afk.yummy.tv.data.account.mapper.toVideoSubscription
import su.afk.yummy.tv.data.account.mapper.toVideoSubscriptions
import su.afk.yummy.tv.data.account.mapper.toVideoSubscriptionsCache
import su.afk.yummy.tv.data.account.network.YaniAccountApi
import su.afk.yummy.tv.domain.account.model.VideoSubscription
import su.afk.yummy.tv.domain.account.repository.VideoSubscriptionRepository

class YaniVideoSubscriptionRepository(
    private val api: YaniAccountApi,
    private val cache: CacheStore,
    private val accountStorage: AccountStorageStore,
    private val json: Json,
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
                    ?: readLegacySubscriptions(userId, language, languageCode)
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
                cache.invalidatePrefix(YaniAccountCacheKeys.subscriptions(userId))
            }
            result
        }

    private suspend fun fetchSubscriptions(
        userId: Int,
        languageCode: String,
    ): List<VideoSubscription> {
        val subscriptions = api.getSubscriptions(userId).mapNotNull { it.toVideoSubscription() }
        accountStorage.saveVideoSubscriptions(
            subscriptions.toVideoSubscriptionsCache(
                userId = userId,
                language = languageCode,
                cachedAt = System.currentTimeMillis(),
            )
        )
        return subscriptions
    }

    private suspend fun readLegacySubscriptions(
        userId: Int,
        language: YaniContentLanguage,
        languageCode: String,
    ): List<VideoSubscription>? {
        val cached = cache.getCached<YaniVideoSubscriptionsResponseDto>(
            key = YaniAccountCacheKeys.subscriptions(userId, language),
            deserialize = { json.decodeFromString(it) },
        ) ?: return null

        val subscriptions = cached.value.response.mapNotNull { it.toVideoSubscription() }
        accountStorage.saveVideoSubscriptions(
            subscriptions.toVideoSubscriptionsCache(
                userId = userId,
                language = languageCode,
                cachedAt = cached.cachedAt,
            )
        )
        return subscriptions
    }
}
