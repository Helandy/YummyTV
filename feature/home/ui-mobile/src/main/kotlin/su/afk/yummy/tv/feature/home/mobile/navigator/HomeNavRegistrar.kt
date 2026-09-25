package su.afk.yummy.tv.feature.home.mobile.navigator

import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import su.afk.yummy.tv.core.designsystem.baseScreen.ScreenNavigator
import su.afk.yummy.tv.core.navigation.manager.INavigationManager
import su.afk.yummy.tv.feature.home.HomeViewModel
import su.afk.yummy.tv.feature.home.IMobileHomeEntry
import su.afk.yummy.tv.feature.home.mobile.HomeMobileScreen
import su.afk.yummy.tv.feature.home.navigator.HomeDestination
import javax.inject.Inject

class HomeNavRegistrar @Inject constructor() : IMobileHomeEntry {
    override fun register(builder: EntryProviderScope<NavKey>, nav: INavigationManager) =
        with(builder) {
            entry<HomeDestination> {
                val viewModel = hiltViewModel<HomeViewModel>()
                ScreenNavigator(viewModel) { state, effect, onEvent ->
                    HomeMobileScreen(state = state, effect = effect, onEvent = onEvent)
                }
            }
        }
}
