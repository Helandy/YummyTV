package su.afk.yummy.tv.feature.collection.deeplink

import androidx.navigation3.runtime.NavKey
import su.afk.yummy.tv.core.deeplink.api.DeepLinkReference
import su.afk.yummy.tv.core.deeplink.api.DeepLinkResolver
import su.afk.yummy.tv.feature.collection.ICollectionNavigator
import javax.inject.Inject

/**
 * Разбирает `yummytv://collection/{id}` и ссылку сайта `https://ru.yummyani.me/catalog/collection/{id}`
 * в экран коллекции.
 */
class CollectionDeepLinkResolver @Inject constructor(
    private val collectionNavigator: ICollectionNavigator,
) : DeepLinkResolver {

    override fun resolve(link: DeepLinkReference): NavKey? {
        val uri = link.uri
        val isAppLink = link.appLinkHost == "collection"
        val isSiteLink = uri.scheme == "https" && uri.path?.startsWith(SITE_PATH_PREFIX) == true
        if (!isAppLink && !isSiteLink) return null
        val id = uri.lastPathSegment?.toIntOrNull() ?: return null
        return collectionNavigator.getCollectionDest(id)
    }

    private companion object {
        const val SITE_PATH_PREFIX = "/catalog/collection/"
    }
}
