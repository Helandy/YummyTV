package su.afk.yummy.tv.feature.pages.mobile.navigator

import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import su.afk.yummy.tv.core.designsystem.baseScreen.ScreenNavigator
import su.afk.yummy.tv.core.navigation.manager.INavigationManager
import su.afk.yummy.tv.feature.pages.IMobileSitePagesEntry
import su.afk.yummy.tv.feature.pages.SitePagesViewModel
import su.afk.yummy.tv.feature.pages.mobile.SitePagesMobileScreen
import su.afk.yummy.tv.feature.pages.navigator.SitePagesDestination
import javax.inject.Inject

class SitePagesNavRegistrar @Inject constructor() : IMobileSitePagesEntry {
    override fun register(builder: EntryProviderScope<NavKey>, nav: INavigationManager) =
        with(builder) {
            entry<SitePagesDestination> {
                ScreenNavigator(hiltViewModel<SitePagesViewModel>()) { state, effect, onEvent ->
                    SitePagesMobileScreen(state, effect, onEvent)
                }
            }
        }
}
