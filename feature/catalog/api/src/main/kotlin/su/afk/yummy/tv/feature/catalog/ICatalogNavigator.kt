package su.afk.yummy.tv.feature.catalog

import androidx.navigation3.runtime.NavKey

interface ICatalogNavigator {
    fun getCatalogDest(initialQuery: String = ""): NavKey
}
