package su.afk.yummy.tv.feature.library.navigator

import androidx.navigation3.runtime.NavKey
import su.afk.yummy.tv.feature.library.ILibraryNavigator

class LibraryNavigator : ILibraryNavigator {
    override fun getLibraryDest(): NavKey = LibraryDestination
}
