package su.afk.yummy.tv.feature.search.navigator

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable
import su.afk.yummy.tv.core.analytics.AnalyticsDestination

@Serializable
data class SearchDestination(val initialQuery: String = "") : NavKey, AnalyticsDestination {
    override val screenName: String = "search"
    override val screenParams: Map<String, String>
        get() = mapOf("has_initial_query" to initialQuery.isNotBlank().toString())
}
