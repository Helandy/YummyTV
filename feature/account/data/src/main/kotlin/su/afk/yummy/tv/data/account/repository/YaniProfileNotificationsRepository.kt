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
import su.afk.yummy.tv.data.account.mapper.toNotificationAnimeEntry
import su.afk.yummy.tv.data.account.mapper.toNotificationCount
import su.afk.yummy.tv.data.account.mapper.toNotificationCounts
import su.afk.yummy.tv.data.account.mapper.toNotificationCountsCache
import su.afk.yummy.tv.data.account.mapper.toNotifications
import su.afk.yummy.tv.data.account.mapper.toNotificationsPageCache
import su.afk.yummy.tv.data.account.network.YaniAccountApi
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
            val stored = accountStorage.getNotifications(userId, languageCode, limit, offset)
            if (stored?.isFresh(ACCOUNT_SHORT_TTL_MS) == true) {
                return@withContext stored.toNotifications()
            }

            try {
                fetchNotifications(userId, languageCode, limit, offset)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                stored?.toNotifications()
                    ?: throw error
            }
        }

    override suspend fun getNotificationCounts(): List<NotificationCount> =
        withContext(Dispatchers.IO) {
            val userId = currentUserId()
            val stored = accountStorage.getNotificationCounts(userId)
            if (stored?.isFresh(ACCOUNT_SHORT_TTL_MS) == true) {
                return@withContext stored.toNotificationCounts()
            }

            try {
                fetchNotificationCounts(userId)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                stored?.toNotificationCounts()
                    ?: throw error
            }
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

    private suspend fun fetchNotifications(
        userId: Int,
        languageCode: String,
        limit: Int,
        offset: Int,
    ): List<ProfileNotification> {
        val notifications =
            api.getNotifications(limit = limit, offset = offset).map { it.toNotification() }
        accountStorage.saveNotifications(
            notifications.toNotificationsPageCache(
                userId = userId,
                language = languageCode,
                limit = limit,
                offset = offset,
                cachedAt = System.currentTimeMillis(),
            )
        )
        return notifications
    }

    private suspend fun fetchNotificationCounts(userId: Int): List<NotificationCount> {
        val counts = api.getNotificationCounts().map { it.toNotificationCount() }
        accountStorage.saveNotificationCounts(
            counts.toNotificationCountsCache(
                userId = userId,
                cachedAt = System.currentTimeMillis(),
            )
        )
        return counts
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
