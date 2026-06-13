package su.afk.yummy.tv.feature.search.navigator

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
data class SearchDestination(val initialQuery: String = "") : NavKey
