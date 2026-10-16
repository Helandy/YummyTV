package su.afk.yummy.tv.feature.catalog.android

import android.app.SearchManager
import android.content.Intent
import su.afk.yummy.tv.core.navigation.manager.INavigationManager
import su.afk.yummy.tv.core.navigation.root.RootTab
import su.afk.yummy.tv.feature.catalog.ICatalogNavigator
import javax.inject.Inject

class SystemSearchIntentHandler @Inject constructor(
    private val nav: INavigationManager,
    private val searchNavigator: ICatalogNavigator,
) {
    fun handle(intent: Intent): Boolean {
        if (intent.action != Intent.ACTION_SEARCH) return false

        val query = intent.getStringExtra(SearchManager.QUERY).orEmpty().trim()
        nav.replaceRoot(
            root = RootTab.CATALOG,
            dest = searchNavigator.getCatalogDest(initialQuery = query),
        )
        return true
    }
}
