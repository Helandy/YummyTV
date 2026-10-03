package su.afk.yummy.tv.data.account.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import su.afk.yummy.tv.core.preferences.settings.YaniAccountSettingsStore
import su.afk.yummy.tv.core.preferences.settings.currentLanguageCode
import su.afk.yummy.tv.core.preferences.settings.currentUserId
import su.afk.yummy.tv.core.storage.account.AccountNotificationAnimeEntry
import su.afk.yummy.tv.core.storage.account.AccountNotificationsPageCache
import su.afk.yummy.tv.core.storage.account.AccountStorage
import su.afk.yummy.tv.core.storage.account.isFresh
import su.afk.yummy.tv.core.storage.offlinefirst.offlineFirstCache
import su.afk.yummy.tv.data.account.network.YaniAccountApi
import su.afk.yummy.tv.data.account.storage.mapper.toNotificationAnimeEntry
import su.afk.yummy.tv.data.account.storage.mapper.toNotificationCounts
import su.afk.yummy.tv.data.account.storage.mapper.toNotificationCountsCache
import su.afk.yummy.tv.data.account.storage.mapper.toNotifications
import su.afk.yummy.tv.data.account.storage.mapper.toNotificationsPageCache
import su.afk.yummy.tv.domain.account.model.NotificationCount
import su.afk.yummy.tv.domain.account.model.ProfileNotification
import su.afk.yummy.tv.domain.account.repository.ProfileNotificationsRepository

class YaniProfileNotificationsRepository(
    private val api: YaniAccountApi,
    private val accountStorage: AccountStorage,
    private val settingsStore: YaniAccountSettingsStore,
) : ProfileNotificationsRepository {
    override suspend fun getNotifications(limit: Int, offset: Int): List<ProfileNotification> =
        withContext(Dispatchers.IO) {
            val userId = settingsStore.currentUserId()
            val languageCode = settingsStore.currentLanguageCode()
            getNotificationsPage(userId, languageCode, limit, offset)
        }

    override suspend fun getNotificationCounts(): List<NotificationCount> =
        withContext(Dispatchers.IO) {
            val userId = settingsStore.currentUserId()
            if (userId <= 0) return@withContext emptyList()
            offlineFirstCache(
                read = { accountStorage.getNotificationCounts(userId) },
                isFresh = { it.isFresh(ACCOUNT_NOTIFICATIONS_TTL_MS) },
                toDomain = { it.toNotificationCounts() },
                fetchAndSave = {
                    val cache = api.getNotificationCounts().toNotificationCountsCache(
                        userId = userId,
                        cachedAt = System.currentTimeMillis(),
                    )
                    accountStorage.saveNotificationCounts(cache)
                    cache
                },
            )
        }

    override suspend fun resolveAnimeIdBySlug(slug: String): Int? =
        withContext(Dispatchers.IO) {
            offlineFirstCache(
                read = { accountStorage.getNotificationAnime(slug) },
                isFresh = { it.isFresh(ACCOUNT_LONG_TTL_MS) },
                toDomain = { it.animeId },
                fetchAndSave = { fetchNotificationAnime(slug) },
            )
        }

    override suspend fun markNotificationRead(id: Int): Boolean =
        withContext(Dispatchers.IO) {
            val userId = settingsStore.currentUserId()
            api.markNotificationRead(id).also {
                invalidateNotifications(userId)
            }
        }

    override suspend fun markAllNotificationsRead(): Boolean =
        withContext(Dispatchers.IO) {
            val userId = settingsStore.currentUserId()
            api.markAllNotificationsRead().also {
                invalidateNotifications(userId)
            }
        }

    override suspend fun deleteNotification(id: Int): Boolean =
        withContext(Dispatchers.IO) {
            val userId = settingsStore.currentUserId()
            api.deleteNotification(id).also {
                invalidateNotifications(userId)
            }
        }

    override suspend fun deleteAllNotifications(): Boolean =
        withContext(Dispatchers.IO) {
            val userId = settingsStore.currentUserId()
            api.deleteAllNotifications().also {
                invalidateNotifications(userId)
            }
        }

    private suspend fun getNotificationsPage(
        userId: Int,
        languageCode: String,
        limit: Int,
        offset: Int,
    ): List<ProfileNotification> = offlineFirstCache(
        read = { accountStorage.getNotifications(userId, languageCode, limit, offset) },
        isFresh = { it.isFresh(ACCOUNT_NOTIFICATIONS_TTL_MS) },
        toDomain = { it.toNotifications() },
        fetchAndSave = { fetchNotifications(userId, languageCode, limit, offset) },
    )

    private suspend fun fetchNotifications(
        userId: Int,
        languageCode: String,
        limit: Int,
        offset: Int,
    ): AccountNotificationsPageCache {
        val cachedAt = System.currentTimeMillis()
        val cache = api.getNotifications(limit = limit, offset = offset).toNotificationsPageCache(
            userId = userId,
            language = languageCode,
            limit = limit,
            offset = offset,
            cachedAt = cachedAt,
        )
        accountStorage.saveNotifications(
            cache,
            prunePagesCachedBefore = cachedAt - ACCOUNT_PAGE_CACHE_RETENTION_MS,
        )
        return cache
    }

    private suspend fun fetchNotificationAnime(slug: String): AccountNotificationAnimeEntry {
        val entry = api.getNotificationAnimeId(slug)
            .toNotificationAnimeEntry(
                slug = slug,
                cachedAt = System.currentTimeMillis(),
            )
        accountStorage.saveNotificationAnime(entry)
        return entry
    }

    private suspend fun invalidateNotifications(userId: Int) {
        if (userId <= 0) return
        accountStorage.deleteNotifications(userId)
        accountStorage.deleteNotificationCounts(userId)
    }
}
