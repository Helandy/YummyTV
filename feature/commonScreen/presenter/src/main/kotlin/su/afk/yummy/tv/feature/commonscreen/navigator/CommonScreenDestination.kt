package su.afk.yummy.tv.feature.commonscreen.navigator

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable
import su.afk.yummy.tv.core.analytics.AnalyticsDestination
import su.afk.yummy.tv.core.model.ErrorItem

object CommonScreenDestination {
    @Serializable
    data class ErrorNavigatorDest(
        val error: ErrorItem
    ) : NavKey, AnalyticsDestination {
        override val screenName: String = "error"
        override val screenParams: Map<String, String>
            get() = buildMap {
                error.code?.let { put("code", it.toString()) }
                error.method?.takeIf { it.isNotBlank() }?.let { put("method", it) }
                put("has_retry", (!error.retryKey.isNullOrBlank()).toString())
            }
    }

    @Serializable
    data class ImageViewDest(
        val imageUrls: List<String>,
        val selectedIndex: Int = 0,
    ) : NavKey, AnalyticsDestination {
        override val screenName: String = "image_view"
        override val screenParams: Map<String, String>
            get() = mapOf(
                "image_count" to imageUrls.size.toString(),
                "selected_index" to selectedIndex.toString(),
            )
    }
}
