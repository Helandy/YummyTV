package su.afk.yummy.tv.feature.library.navigator

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable
import su.afk.yummy.tv.core.analytics.AnalyticsDestination

@Serializable
data object LibraryDestination : NavKey, AnalyticsDestination {
    override val screenName: String = "library"
}
