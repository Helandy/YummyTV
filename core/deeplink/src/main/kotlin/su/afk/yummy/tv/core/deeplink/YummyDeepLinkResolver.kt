package su.afk.yummy.tv.core.deeplink

import android.net.Uri
import androidx.navigation3.runtime.NavKey
import su.afk.yummy.tv.feature.details.IDetailsNavigator
import javax.inject.Inject

internal class YummyDeepLinkResolver @Inject constructor(
    private val detailsNavigator: IDetailsNavigator,
) : DeepLinkResolver {

    override fun resolve(uri: Uri): NavKey? {
        if (uri.scheme != "yummytv") return null
        return when (uri.host) {
            "details" -> {
                val animeId = uri.lastPathSegment?.toIntOrNull() ?: return null
                detailsNavigator.getDetailsDest(animeId)
            }
            else -> null
        }
    }
}
