package su.afk.yummy.tv.feature.home.navigator

import androidx.navigation3.runtime.NavKey
import su.afk.yummy.tv.feature.home.IHomeNavigator

class HomeNavigator : IHomeNavigator {
    override fun getHomeDest(): NavKey = HomeDestination
}
