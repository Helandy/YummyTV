package su.afk.yummy.tv.feature.collection.navigator

import androidx.navigation3.runtime.NavKey
import su.afk.yummy.tv.feature.collection.ICollectionNavigator
import javax.inject.Inject

class CollectionNavigator @Inject constructor() : ICollectionNavigator {
    override fun getCollectionDest(collectionId: Int): NavKey = CollectionDestination(collectionId)
}
