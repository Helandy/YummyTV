package su.afk.yummy.tv.feature.search.navigator

import androidx.navigation3.runtime.NavKey
import su.afk.yummy.tv.feature.search.ISearchNavigator
import javax.inject.Inject

class SearchNavigator @Inject constructor() : ISearchNavigator {
    override fun getSearchDest(): NavKey = SearchDestination
}
