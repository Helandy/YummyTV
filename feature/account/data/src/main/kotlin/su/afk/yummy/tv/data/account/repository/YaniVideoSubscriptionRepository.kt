package su.afk.yummy.tv.data.account.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import su.afk.yummy.tv.core.preferences.settings.SettingsStore
import su.afk.yummy.tv.core.storage.cache.CacheStore
import su.afk.yummy.tv.data.account.dto.YaniVideoSubscriptionsResponseDto
import su.afk.yummy.tv.data.account.mapper.toVideoSubscription
import su.afk.yummy.tv.data.account.network.YaniAccountApi
import su.afk.yummy.tv.domain.account.model.VideoSubscription
import su.afk.yummy.tv.domain.account.repository.VideoSubscriptionRepository

class YaniVideoSubscriptionRepository(
    private val api: YaniAccountApi,
    private val cache: CacheStore,
    private val json: Json,
    private val settingsStore: SettingsStore,
) : VideoSubscriptionRepository {

    override suspend fun getSubscriptions(userId: Int): List<VideoSubscription> =
        withContext(Dispatchers.IO) {
            val language = settingsStore.yaniContentLanguage.first()
            cache.getOrFetch(
                key = YaniAccountCacheKeys.subscriptions(userId, language),
                ttlMs = ACCOUNT_SHORT_TTL_MS,
                serialize = { dto: YaniVideoSubscriptionsResponseDto -> json.encodeToString(dto) },
                deserialize = { json.decodeFromString(it) },
                fetch = { YaniVideoSubscriptionsResponseDto(response = api.getSubscriptions(userId)) },
            ).response.mapNotNull { it.toVideoSubscription() }
        }

    override suspend fun setSubscribed(videoId: Int, subscribed: Boolean): Boolean =
        withContext(Dispatchers.IO) {
            val userId = settingsStore.yaniUserId.first()
            val result = if (subscribed) {
                api.setSubscribed(videoId)
            } else {
                api.removeSubscribed(videoId)
            }
            if (userId > 0) cache.invalidatePrefix(YaniAccountCacheKeys.subscriptions(userId))
            result
        }
}
