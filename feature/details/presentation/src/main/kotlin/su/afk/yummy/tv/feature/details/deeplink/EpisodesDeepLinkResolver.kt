package su.afk.yummy.tv.feature.details.deeplink

import androidx.navigation3.runtime.NavKey
import su.afk.yummy.tv.core.deeplink.api.DeepLinkReference
import su.afk.yummy.tv.core.deeplink.api.DeepLinkResolver
import su.afk.yummy.tv.feature.details.IDetailsNavigator
import javax.inject.Inject

/** Разбирает `yummytv://episodes/{animeId}` сразу в экран выбора серии тайтла. */
class EpisodesDeepLinkResolver @Inject constructor(
    private val detailsNavigator: IDetailsNavigator,
) : DeepLinkResolver {

    override fun resolve(link: DeepLinkReference): NavKey? {
        if (link.appLinkHost != "episodes") return null
        val animeId = link.uri.lastPathSegment?.toIntOrNull() ?: return null
        return detailsNavigator.getEpisodesDest(animeId)
    }
}
