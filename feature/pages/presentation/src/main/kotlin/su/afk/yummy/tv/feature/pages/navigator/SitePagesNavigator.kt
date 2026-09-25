package su.afk.yummy.tv.feature.pages.navigator

import androidx.navigation3.runtime.NavKey
import su.afk.yummy.tv.feature.pages.ISitePagesNavigator
import javax.inject.Inject

class SitePagesNavigator @Inject constructor() : ISitePagesNavigator {
    override fun pages(): NavKey = SitePagesDestination
}
