package su.afk.yummy.tv.feature.catalog.navigator

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
data class CatalogDestination(val initialQuery: String = "") : NavKey
