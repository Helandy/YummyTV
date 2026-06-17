package su.afk.yummy.tv.data.account.repository

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import su.afk.yummy.tv.core.preferences.settings.SettingsStore
import su.afk.yummy.tv.core.storage.account.AccountNotificationAnimeEntry
import su.afk.yummy.tv.core.storage.account.AccountStorageStore
import su.afk.yummy.tv.core.storage.account.isFresh
import su.afk.yummy.tv.data.account.mapper.toNotification
import su.afk.yummy.tv.data.account.network.YaniAccountApi
import su.afk.yummy.tv.data.account.storage.mapper.toNotificationAnimeEntry
import su.afk.yummy.tv.data.account.storage.mapper.toNotifications
import su.afk.yummy.tv.data.account.storage.mapper.toNotificationsPageCache
import su.afk.yummy.tv.domain.account.model.NotificationCount
import su.afk.yummy.tv.domain.account.model.ProfileNotification
import su.afk.yummy.tv.domain.account.repository.ProfileNotificationsRepository

class YaniProfileNotificationsRepository(
    private val api: YaniAccountApi,
    private val accountStorage: AccountStorageStore,
    private val settingsStore: SettingsStore,
) : ProfileNotificationsRepository {
    override suspend fun getNotifications(limit: Int, offset: Int): List<ProfileNotification> =
        withContext(Dispatchers.IO) {
            val userId = currentUserId()
            val language = settingsStore.yaniContentLanguage.first()
            val languageCode = language.apiCode
            getNotificationsPage(userId, languageCode, limit, offset)
        }

    override suspend fun getNotificationCounts(): List<NotificationCount> =
        withContext(Dispatchers.IO) {
            val userId = currentUserId()
            if (userId <= 0) return@withContext emptyList()
            val languageCode = settingsStore.yaniContentLanguage.first().apiCode
            loadUnreadNotificationCounts(userId, languageCode)
        }

    override suspend fun resolveAnimeIdBySlug(slug: String): Int? =
        withContext(Dispatchers.IO) {
            val stored = accountStorage.getNotificationAnime(slug)
            if (stored?.isFresh(ACCOUNT_LONG_TTL_MS) == true) {
                return@withContext stored.animeId
            }

            try {
                fetchNotificationAnime(slug).animeId
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                if (stored != null) {
                    stored.animeId
                } else {
                    throw error
                }
            }
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

    private suspend fun getNotificationsPage(
        userId: Int,
        languageCode: String,
        limit: Int,
        offset: Int,
    ): List<ProfileNotification> {
        val stored = accountStorage.getNotifications(userId, languageCode, limit, offset)
        if (stored?.isFresh(ACCOUNT_SHORT_TTL_MS) == true) {
            return stored.toNotifications()
        }

        return try {
            fetchNotifications(userId, languageCode, limit, offset)
        } catch (error: CancellationException) {
            throw error
        } catch (error: Throwable) {
            stored?.toNotifications()
                ?: throw error
        }
    }

    private suspend fun loadUnreadNotificationCounts(
        userId: Int,
        languageCode: String,
    ): List<NotificationCount> {
        val counts = linkedMapOf<String, Int>()
        var offset = 0
        var scanned = 0

        while (scanned < NOTIFICATION_UNREAD_SCAN_MAX) {
            val limit = minOf(
                NOTIFICATION_UNREAD_SCAN_PAGE_SIZE,
                NOTIFICATION_UNREAD_SCAN_MAX - scanned,
            )
            val page = getNotificationsPage(userId, languageCode, limit, offset)
            if (page.isEmpty()) break

            page.filterNot(ProfileNotification::viewed).forEach { notification ->
                counts[notification.type] = (counts[notification.type] ?: 0) + 1
            }
            scanned += page.size
            if (page.size < limit) break
            offset += page.size
        }

        return counts.map { (type, count) -> NotificationCount(type = type, count = count) }
    }

    private suspend fun fetchNotifications(
        userId: Int,
        languageCode: String,
        limit: Int,
        offset: Int,
    ): List<ProfileNotification> {
        val notifications =
            api.getNotifications(limit = limit, offset = offset).map { it.toNotification() }
        val cachedAt = System.currentTimeMillis()
        accountStorage.saveNotifications(
            notifications.toNotificationsPageCache(
                userId = userId,
                language = languageCode,
                limit = limit,
                offset = offset,
                cachedAt = cachedAt,
            ),
            prunePagesCachedBefore = cachedAt - ACCOUNT_PAGE_CACHE_RETENTION_MS,
        )
        return notifications
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

private const val NOTIFICATION_UNREAD_SCAN_PAGE_SIZE = 100
private const val NOTIFICATION_UNREAD_SCAN_MAX = 1_000
