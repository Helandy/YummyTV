package su.afk.yummy.tv.feature.collection

import androidx.navigation3.runtime.NavKey

interface ICollectionNavigator {
    fun getCollectionDest(collectionId: Int): NavKey
    fun getCollectionsCatalogDest(): NavKey
}
