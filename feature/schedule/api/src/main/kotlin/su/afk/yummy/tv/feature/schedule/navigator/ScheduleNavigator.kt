package su.afk.yummy.tv.feature.schedule.navigator

import androidx.navigation3.runtime.NavKey
import su.afk.yummy.tv.feature.schedule.IScheduleNavigator

class ScheduleNavigator : IScheduleNavigator {
    override fun getScheduleDest(): NavKey = ScheduleDestination
}
