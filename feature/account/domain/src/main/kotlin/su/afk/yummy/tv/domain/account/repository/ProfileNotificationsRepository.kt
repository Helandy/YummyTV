package su.afk.yummy.tv.domain.account.repository

import su.afk.yummy.tv.domain.account.model.NotificationCount
import su.afk.yummy.tv.domain.account.model.ProfileNotification

interface ProfileNotificationsRepository {
    suspend fun getNotifications(limit: Int = 20, offset: Int = 0): List<ProfileNotification>
    suspend fun getNotificationCounts(): List<NotificationCount>

    /** Первая страница уведомлений из локального кэша независимо от его возраста; `null`, если её там нет. */
    suspend fun getCachedNotifications(limit: Int = 20): List<ProfileNotification>?
    suspend fun resolveAnimeIdBySlug(slug: String): Int?
    suspend fun markNotificationRead(id: Int): Boolean
    suspend fun markAllNotificationsRead(): Boolean
    suspend fun deleteNotification(id: Int): Boolean
    suspend fun deleteAllNotifications(): Boolean
}
