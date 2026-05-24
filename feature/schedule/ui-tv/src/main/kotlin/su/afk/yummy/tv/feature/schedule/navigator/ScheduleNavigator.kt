package su.afk.yummy.tv.feature.schedule.navigator

import androidx.navigation3.runtime.NavKey
import su.afk.yummy.tv.feature.schedule.IScheduleNavigator
import javax.inject.Inject

class ScheduleNavigator @Inject constructor() : IScheduleNavigator {
    override fun getScheduleDest(): NavKey = ScheduleDestination
}
