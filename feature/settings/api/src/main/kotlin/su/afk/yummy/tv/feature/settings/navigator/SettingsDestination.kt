package su.afk.yummy.tv.feature.settings.navigator

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable
import su.afk.yummy.tv.core.analytics.AnalyticsDestination

@Serializable
data object SettingsDestination : NavKey, AnalyticsDestination {
    override val screenName: String = "settings"
}

@Serializable
data object SettingsDetailsButtonOrderDestination : NavKey, AnalyticsDestination {
    override val screenName: String = "settings_details_button_order"
}
