package su.afk.yummy.tv.feature.collection.navigator

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
data class CollectionDestination(val collectionId: Int) : NavKey
