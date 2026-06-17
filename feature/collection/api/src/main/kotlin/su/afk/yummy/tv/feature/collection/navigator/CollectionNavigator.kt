package su.afk.yummy.tv.feature.collection.navigator

import androidx.navigation3.runtime.NavKey
import su.afk.yummy.tv.feature.collection.ICollectionNavigator

class CollectionNavigator : ICollectionNavigator {
    override fun getCollectionDest(collectionId: Int): NavKey = CollectionDestination(collectionId)
    override fun getCollectionsCatalogDest(): NavKey = CollectionsCatalogDestination
}
