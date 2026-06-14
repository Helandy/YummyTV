package su.afk.yummy.tv.feature.schedule.navigator

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable
import su.afk.yummy.tv.core.analytics.AnalyticsDestination

@Serializable
data object ScheduleDestination : NavKey, AnalyticsDestination {
    override val screenName: String = "schedule"
}
