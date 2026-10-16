package su.afk.yummy.tv.feature.catalog.navigator

import androidx.navigation3.runtime.NavKey
import su.afk.yummy.tv.feature.catalog.ICatalogNavigator
import javax.inject.Inject

class CatalogNavigator @Inject constructor() : ICatalogNavigator {
    override fun getCatalogDest(initialQuery: String): NavKey =
        CatalogDestination(initialQuery = initialQuery)
}
