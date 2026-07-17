package su.afk.yummy.tv.feature.pages.mobile.navigator

import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.ScreenNavigator
import su.afk.yummy.tv.core.navigation.NavRegistrar
import su.afk.yummy.tv.core.navigation.NavigationManager
import su.afk.yummy.tv.feature.pages.SitePagesMobileScreen
import su.afk.yummy.tv.feature.pages.SitePagesViewModel
import su.afk.yummy.tv.feature.pages.navigator.SitePagesDestination

class SitePagesNavRegistrar : NavRegistrar {
    override fun register(builder: EntryProviderScope<NavKey>, nav: NavigationManager) =
        with(builder) {
            entry<SitePagesDestination> {
                ScreenNavigator(hiltViewModel<SitePagesViewModel>()) { state, effect, onEvent ->
                    SitePagesMobileScreen(state, effect, onEvent)
                }
            }
        }
}
