package su.afk.yummy.tv.feature.home.deeplink

import androidx.navigation3.runtime.NavKey
import su.afk.yummy.tv.core.deeplink.api.DeepLinkReference
import su.afk.yummy.tv.core.deeplink.api.DeepLinkResolver
import su.afk.yummy.tv.feature.home.navigator.ContinueWatchingLaunchDestination
import javax.inject.Inject

/** Разбирает `yummytv://continue/{animeId}` (ярлыки, виджет) в запуск «продолжить просмотр». */
class ContinueWatchingDeepLinkResolver @Inject constructor() : DeepLinkResolver {

    override fun resolve(link: DeepLinkReference): NavKey? {
        if (link.appLinkHost != "continue") return null
        val animeId = link.uri.lastPathSegment?.toIntOrNull() ?: return null
        return ContinueWatchingLaunchDestination(animeId)
    }
}
