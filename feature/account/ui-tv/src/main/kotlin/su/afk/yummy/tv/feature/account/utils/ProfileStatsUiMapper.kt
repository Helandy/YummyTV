package su.afk.yummy.tv.feature.account.utils

import androidx.compose.runtime.Composable
import su.afk.yummy.tv.feature.account.account.model.ProfileStatsValueType

@Composable
internal fun ProfileStatsValueType.totalLabel(value: Long): String =
    when (this) {
        ProfileStatsValueType.DURATION -> value.toProfileHoursLabel()
        ProfileStatsValueType.COUNT -> value.toString()
    }

@Composable
internal fun ProfileStatsValueType.valueLabel(value: Long): String =
    when (this) {
        ProfileStatsValueType.DURATION -> value.toProfileHoursLabel()
        ProfileStatsValueType.COUNT -> value.toString()
    }
