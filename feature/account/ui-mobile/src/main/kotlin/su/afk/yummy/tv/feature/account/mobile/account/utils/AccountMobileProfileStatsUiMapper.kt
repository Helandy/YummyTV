package su.afk.yummy.tv.feature.account.mobile.account.utils

import androidx.compose.runtime.Composable
import su.afk.yummy.tv.feature.account.mobile.account.model.AccountMobileProfileStatsValueType

@Composable
internal fun AccountMobileProfileStatsValueType.totalLabel(value: Long): String =
    when (this) {
        AccountMobileProfileStatsValueType.DURATION -> value.toProfileHoursLabel()
        AccountMobileProfileStatsValueType.COUNT -> value.toString()
    }

@Composable
internal fun AccountMobileProfileStatsValueType.valueLabel(value: Long): String =
    when (this) {
        AccountMobileProfileStatsValueType.DURATION -> value.toProfileHoursLabel()
        AccountMobileProfileStatsValueType.COUNT -> value.toString()
    }
