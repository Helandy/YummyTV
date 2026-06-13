package su.afk.yummy.tv.domain.account.usecase

import su.afk.yummy.tv.domain.account.model.NotificationCount
import su.afk.yummy.tv.domain.account.repository.ProfileNotificationsRepository

/** Calculates unread notification counts grouped by notification type. */
class GetNotificationCountsUseCase(private val repository: ProfileNotificationsRepository) {
    suspend operator fun invoke(): List<NotificationCount> {
        val counts = mutableMapOf<String, Int>()
        var offset = 0

        repeat(MAX_NOTIFICATION_COUNT_PAGES) {
            val notifications = repository.getNotifications(
                limit = NOTIFICATION_COUNT_PAGE_LIMIT,
                offset = offset,
            )
            notifications
                .asSequence()
                .filterNot { it.viewed }
                .forEach { notification ->
                    counts[notification.type] = counts.getOrDefault(notification.type, 0) + 1
                }
            if (notifications.size < NOTIFICATION_COUNT_PAGE_LIMIT) {
                return counts.toNotificationCounts()
            }
            offset += NOTIFICATION_COUNT_PAGE_LIMIT
        }

        return counts.toNotificationCounts()
    }

    private fun Map<String, Int>.toNotificationCounts(): List<NotificationCount> =
        map { (type, count) -> NotificationCount(type = type, count = count) }

    private companion object {
        const val NOTIFICATION_COUNT_PAGE_LIMIT = 100
        const val MAX_NOTIFICATION_COUNT_PAGES = 10
    }
}
