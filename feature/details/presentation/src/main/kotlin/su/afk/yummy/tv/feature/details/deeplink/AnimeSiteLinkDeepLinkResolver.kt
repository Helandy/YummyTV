package su.afk.yummy.tv.feature.details.deeplink

import androidx.navigation3.runtime.NavKey
import su.afk.yummy.tv.core.deeplink.api.DeepLinkReference
import su.afk.yummy.tv.core.deeplink.api.DeepLinkResolver
import su.afk.yummy.tv.feature.details.navigator.DetailsSiteLinkDestination
import su.afk.yummy.tv.feature.details.utils.toAnimeSiteSlug
import javax.inject.Inject

/** Разбирает ссылку сайта `https://ru.yummyani.me/catalog/item/{slug}` в экран тайтла. */
class AnimeSiteLinkDeepLinkResolver @Inject constructor() : DeepLinkResolver {

    override fun resolve(link: DeepLinkReference): NavKey? =
        link.uri.toAnimeSiteSlug()?.let(::DetailsSiteLinkDestination)
}
