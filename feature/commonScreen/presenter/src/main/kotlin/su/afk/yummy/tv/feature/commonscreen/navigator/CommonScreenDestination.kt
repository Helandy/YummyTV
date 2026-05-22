package su.afk.yummy.tv.feature.commonscreen.navigator

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable
import su.afk.yummy.tv.core.model.ErrorItem

object CommonScreenDestination {
    @Serializable
    data class ErrorNavigatorDest(
        val error: ErrorItem
    ) : NavKey

    @Serializable
    data class ImageViewDest(
        val imageUrls: List<String>,
        val selectedIndex: Int = 0,
    ) : NavKey
}
