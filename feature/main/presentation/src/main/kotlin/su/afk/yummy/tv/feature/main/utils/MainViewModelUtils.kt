package su.afk.yummy.tv.feature.main.utils

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

internal suspend fun Flow<Long>.firstOrZero(): Long =
    first()
