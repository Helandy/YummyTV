package su.afk.yummy.tv.data.account.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import su.afk.yummy.tv.core.preferences.settings.SettingsStore
import su.afk.yummy.tv.core.storage.cache.CacheStore
import su.afk.yummy.tv.data.account.dto.YaniNotificationAnimeDto
import su.afk.yummy.tv.data.account.dto.YaniNotificationAnimeResponseDto
import su.afk.yummy.tv.data.account.dto.YaniNotificationCountsResponseDto
import su.afk.yummy.tv.data.account.dto.YaniNotificationsResponseDto
import su.afk.yummy.tv.data.account.mapper.toNotification
import su.afk.yummy.tv.data.account.mapper.toNotificationCount
import su.afk.yummy.tv.data.account.network.YaniAccountApi
import su.afk.yummy.tv.domain.account.model.NotificationCount
import su.afk.yummy.tv.domain.account.model.ProfileNotification
import su.afk.yummy.tv.domain.account.repository.ProfileNotificationsRepository

class YaniProfileNotificationsRepository(
    private val api: YaniAccountApi,
    private val cache: CacheStore,
    private val json: Json,
    private val settingsStore: SettingsStore,
) : ProfileNotificationsRepository {
    override suspend fun getNotifications(limit: Int, offset: Int): List<ProfileNotification> =
        withContext(Dispatchers.IO) {
            val userId = currentUserId()
            val language = settingsStore.yaniContentLanguage.first()
            cache.getOrFetch(
                key = YaniAccountCacheKeys.notifications(userId, limit, offset, language),
                ttlMs = ACCOUNT_SHORT_TTL_MS,
                serialize = { dto: YaniNotificationsResponseDto -> json.encodeToString(dto) },
                deserialize = { json.decodeFromString(it) },
                fetch = {
                    YaniNotificationsResponseDto(
                        response = api.getNotifications(
                            limit = limit,
                            offset = offset
                        )
                    )
                },
            ).response.map { it.toNotification() }
        }

    override suspend fun getNotificationCounts(): List<NotificationCount> =
        withContext(Dispatchers.IO) {
            val userId = currentUserId()
            cache.getOrFetch(
                key = YaniAccountCacheKeys.notificationCounts(userId),
                ttlMs = ACCOUNT_SHORT_TTL_MS,
                serialize = { dto: YaniNotificationCountsResponseDto -> json.encodeToString(dto) },
                deserialize = { json.decodeFromString(it) },
                fetch = { YaniNotificationCountsResponseDto(response = api.getNotificationCounts()) },
            ).response.map { it.toNotificationCount() }
        }

    override suspend fun resolveAnimeIdBySlug(slug: String): Int? =
        withContext(Dispatchers.IO) {
            cache.getOrFetch(
                key = YaniAccountCacheKeys.notificationAnime(slug),
                ttlMs = ACCOUNT_LONG_TTL_MS,
                serialize = { dto: YaniNotificationAnimeResponseDto -> json.encodeToString(dto) },
                deserialize = { json.decodeFromString(it) },
                fetch = {
                    YaniNotificationAnimeResponseDto(
                        response = YaniNotificationAnimeDto(
                            animeId = api.getNotificationAnimeId(slug),
                        )
                    )
                },
            ).response.animeId
        }

    override suspend fun markNotificationRead(id: Int): Boolean =
        withContext(Dispatchers.IO) {
            val userId = currentUserId()
            api.markNotificationRead(id).also {
                invalidateNotifications(userId)
            }
        }

    override suspend fun markAllNotificationsRead(): Boolean =
        withContext(Dispatchers.IO) {
            val userId = currentUserId()
            api.markAllNotificationsRead().also {
                invalidateNotifications(userId)
            }
        }

    override suspend fun deleteNotification(id: Int): Boolean =
        withContext(Dispatchers.IO) {
            val userId = currentUserId()
            api.deleteNotification(id).also {
                invalidateNotifications(userId)
            }
        }

    private suspend fun currentUserId(): Int =
        settingsStore.yaniUserId.first()

    private suspend fun invalidateNotifications(userId: Int) {
        if (userId <= 0) return
        cache.invalidatePrefix("account_user_${userId}_notifications_")
        cache.invalidate(YaniAccountCacheKeys.notificationCounts(userId))
    }
}
