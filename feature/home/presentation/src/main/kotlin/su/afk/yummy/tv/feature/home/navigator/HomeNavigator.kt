package su.afk.yummy.tv.feature.home.navigator

import androidx.navigation3.runtime.NavKey
import su.afk.yummy.tv.feature.home.IHomeNavigator
import javax.inject.Inject

class HomeNavigator @Inject constructor() : IHomeNavigator {
    override fun getHomeDest(): NavKey = HomeDestination
}
