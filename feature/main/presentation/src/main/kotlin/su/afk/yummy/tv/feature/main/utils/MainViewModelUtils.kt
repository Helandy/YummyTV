package su.afk.yummy.tv.feature.main.utils

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

internal const val NOTIFICATION_REFRESH_INTERVAL_MS = 5 * 60 * 1000L

internal suspend fun Flow<Long>.firstOrZero(): Long =
    first()
