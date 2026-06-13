package su.afk.yummy.tv.android.search

import android.app.SearchManager
import android.content.Intent
import su.afk.yummy.tv.core.navigation.NavigationManager
import su.afk.yummy.tv.core.navigation.root.RootTab
import su.afk.yummy.tv.feature.search.ISearchNavigator
import javax.inject.Inject

class SystemSearchIntentHandler @Inject constructor(
    private val nav: NavigationManager,
    private val searchNavigator: ISearchNavigator,
) {
    fun handle(intent: Intent): Boolean {
        if (intent.action != Intent.ACTION_SEARCH) return false

        val query = intent.getStringExtra(SearchManager.QUERY).orEmpty().trim()
        nav.replaceRoot(
            root = RootTab.SEARCH,
            dest = searchNavigator.getSearchDest(initialQuery = query),
        )
        return true
    }
}
