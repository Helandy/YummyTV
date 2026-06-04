package su.afk.yummy.tv.feature.schedule.view

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import su.afk.yummy.tv.feature.schedule.*
import su.afk.yummy.tv.feature.schedule.model.ScheduleRemainingLabels

@Composable
internal fun scheduleRemainingLabels(): ScheduleRemainingLabels =
    ScheduleRemainingLabels(
        dayOne = stringResource(R.string.schedule_day_one),
        dayFew = stringResource(R.string.schedule_day_few),
        dayMany = stringResource(R.string.schedule_day_many),
        hourOne = stringResource(R.string.schedule_hour_one),
        hourFew = stringResource(R.string.schedule_hour_few),
        hourMany = stringResource(R.string.schedule_hour_many),
        minuteOne = stringResource(R.string.schedule_minute_one),
        minuteFew = stringResource(R.string.schedule_minute_few),
        minuteMany = stringResource(R.string.schedule_minute_many),
        lessThanMinute = stringResource(R.string.schedule_less_than_minute),
    )
