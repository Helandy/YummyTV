package su.afk.yummy.tv.feature.watchlater.navigator

import androidx.navigation3.runtime.NavKey
import su.afk.yummy.tv.feature.watchlater.IWatchLaterNavigator
import javax.inject.Inject

class WatchLaterNavigator @Inject constructor() : IWatchLaterNavigator {
    override fun getWatchLaterDest(): NavKey = WatchLaterDestination
}
