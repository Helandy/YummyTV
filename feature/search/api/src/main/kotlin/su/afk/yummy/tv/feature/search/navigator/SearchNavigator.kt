package su.afk.yummy.tv.feature.search.navigator

import androidx.navigation3.runtime.NavKey
import su.afk.yummy.tv.feature.search.ISearchNavigator

class SearchNavigator : ISearchNavigator {
    override fun getSearchDest(): NavKey = SearchDestination
}
