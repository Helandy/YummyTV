package su.afk.yummy.tv.feature.main.utils

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlin.time.Duration.Companion.minutes

internal val NOTIFICATION_REFRESH_INTERVAL = 5.minutes

internal suspend fun Flow<Long>.firstOrZero(): Long =
    first()
