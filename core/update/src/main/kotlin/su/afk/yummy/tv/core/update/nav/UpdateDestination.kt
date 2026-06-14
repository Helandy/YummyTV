package su.afk.yummy.tv.core.update.nav

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable
import su.afk.yummy.tv.core.analytics.AnalyticsDestination

@Serializable
data class UpdateDestination(
    val version: String,
    val apkUrl: String,
    val changelog: String,
) : NavKey, AnalyticsDestination {
    override val screenName: String = "update"
    override val screenParams: Map<String, String>
        get() = mapOf("version" to version)
}
