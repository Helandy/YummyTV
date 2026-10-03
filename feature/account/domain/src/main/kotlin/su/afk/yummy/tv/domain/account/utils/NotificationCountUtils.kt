package su.afk.yummy.tv.domain.account.utils

import su.afk.yummy.tv.domain.account.model.NotificationCount

internal fun Map<String, Int>.toNotificationCounts(): List<NotificationCount> =
    map { (type, count) -> NotificationCount(type = type, count = count) }
