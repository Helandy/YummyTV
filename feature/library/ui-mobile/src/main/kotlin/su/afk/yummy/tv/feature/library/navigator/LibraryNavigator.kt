package su.afk.yummy.tv.feature.library.navigator

import androidx.navigation3.runtime.NavKey
import su.afk.yummy.tv.feature.library.ILibraryNavigator
import javax.inject.Inject

class LibraryNavigator @Inject constructor() : ILibraryNavigator {
    override fun getLibraryDest(): NavKey = LibraryDestination
}
