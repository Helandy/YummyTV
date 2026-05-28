package su.afk.yummy.tv.data.account.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import su.afk.yummy.tv.data.account.mapper.toNotification
import su.afk.yummy.tv.data.account.mapper.toNotificationCount
import su.afk.yummy.tv.data.account.network.YaniAccountApi
import su.afk.yummy.tv.domain.account.model.NotificationCount
import su.afk.yummy.tv.domain.account.model.ProfileNotification
import su.afk.yummy.tv.domain.account.repository.ProfileNotificationsRepository

class YaniProfileNotificationsRepository(
    private val api: YaniAccountApi,
) : ProfileNotificationsRepository {
    override suspend fun getNotifications(limit: Int, offset: Int): List<ProfileNotification> =
        withContext(Dispatchers.IO) {
            api.getNotifications(limit = limit, offset = offset).map { it.toNotification() }
        }

    override suspend fun getNotificationCounts(): List<NotificationCount> =
        withContext(Dispatchers.IO) {
            api.getNotificationCounts().map { it.toNotificationCount() }
        }

    override suspend fun resolveAnimeIdBySlug(slug: String): Int? =
        withContext(Dispatchers.IO) {
            runCatching { api.getNotificationAnimeId(slug) }.getOrNull()
        }

    override suspend fun markNotificationRead(id: Int): Boolean =
        withContext(Dispatchers.IO) { api.markNotificationRead(id) }

    override suspend fun markAllNotificationsRead(): Boolean =
        withContext(Dispatchers.IO) { api.markAllNotificationsRead() }

    override suspend fun deleteNotification(id: Int): Boolean =
        withContext(Dispatchers.IO) { api.deleteNotification(id) }
}
