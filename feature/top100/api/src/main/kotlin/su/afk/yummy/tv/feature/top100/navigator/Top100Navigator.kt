package su.afk.yummy.tv.feature.top100.navigator

import androidx.navigation3.runtime.NavKey
import su.afk.yummy.tv.feature.top100.ITop100Navigator

class Top100Navigator : ITop100Navigator {
    override fun getTop100Dest(): NavKey = Top100Destination
}
