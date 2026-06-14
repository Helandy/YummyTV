package su.afk.yummy.tv.feature.top.navigator

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable
import su.afk.yummy.tv.core.analytics.AnalyticsDestination

@Serializable
data object TopDestination : NavKey, AnalyticsDestination {
    override val screenName: String = "top"
}
