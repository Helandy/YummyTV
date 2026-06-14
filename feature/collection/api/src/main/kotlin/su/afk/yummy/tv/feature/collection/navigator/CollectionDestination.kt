package su.afk.yummy.tv.feature.collection.navigator

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable
import su.afk.yummy.tv.core.analytics.AnalyticsDestination

@Serializable
data class CollectionDestination(val collectionId: Int) : NavKey, AnalyticsDestination {
    override val screenName: String = "collection"
    override val screenParams: Map<String, String>
        get() = mapOf("collection_id" to collectionId.toString())
}
