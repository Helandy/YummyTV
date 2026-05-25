package su.afk.yummy.tv.domain.account.repository

import su.afk.yummy.tv.domain.account.model.NotificationCount
import su.afk.yummy.tv.domain.account.model.ProfileNotification

interface ProfileNotificationsRepository {
    suspend fun getNotifications(limit: Int = 20, offset: Int = 0): List<ProfileNotification>
    suspend fun getNotificationCounts(): List<NotificationCount>
    suspend fun markNotificationRead(id: Int): Boolean
    suspend fun markAllNotificationsRead(): Boolean
    suspend fun deleteNotification(id: Int): Boolean
}
