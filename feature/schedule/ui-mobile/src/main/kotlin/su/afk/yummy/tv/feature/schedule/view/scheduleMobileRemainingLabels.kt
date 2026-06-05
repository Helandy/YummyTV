package su.afk.yummy.tv.feature.schedule.view

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import su.afk.yummy.tv.feature.schedule.mobile.R
import su.afk.yummy.tv.feature.schedule.utils.ScheduleMobileRemainingLabels

@Composable
internal fun scheduleMobileRemainingLabels(): ScheduleMobileRemainingLabels =
    ScheduleMobileRemainingLabels(
        dayOne = stringResource(R.string.schedule_mobile_day_one),
        dayFew = stringResource(R.string.schedule_mobile_day_few),
        dayMany = stringResource(R.string.schedule_mobile_day_many),
        hourOne = stringResource(R.string.schedule_mobile_hour_one),
        hourFew = stringResource(R.string.schedule_mobile_hour_few),
        hourMany = stringResource(R.string.schedule_mobile_hour_many),
        minuteOne = stringResource(R.string.schedule_mobile_minute_one),
        minuteFew = stringResource(R.string.schedule_mobile_minute_few),
        minuteMany = stringResource(R.string.schedule_mobile_minute_many),
        lessThanMinute = stringResource(R.string.schedule_mobile_less_than_minute),
    )
