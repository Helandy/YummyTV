package su.afk.yummy.tv.feature.top.navigator

import androidx.navigation3.runtime.NavKey
import su.afk.yummy.tv.feature.top.ITopNavigator
import javax.inject.Inject

class TopNavigator @Inject constructor() : ITopNavigator {
    override fun getTopDest(): NavKey = TopDestination
}
