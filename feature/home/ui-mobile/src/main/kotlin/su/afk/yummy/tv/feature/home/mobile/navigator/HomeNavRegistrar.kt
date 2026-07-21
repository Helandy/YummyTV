package su.afk.yummy.tv.feature.home.mobile.navigator

import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.ScreenNavigator
import su.afk.yummy.tv.core.navigation.NavRegistrar
import su.afk.yummy.tv.core.navigation.NavigationManager
import su.afk.yummy.tv.feature.home.HomeViewModel
import su.afk.yummy.tv.feature.home.mobile.HomeMobileScreen
import su.afk.yummy.tv.feature.home.navigator.HomeDestination
import javax.inject.Inject

class HomeNavRegistrar @Inject constructor() : NavRegistrar {
    override fun register(builder: EntryProviderScope<NavKey>, nav: NavigationManager) =
        with(builder) {
            entry<HomeDestination> {
                val viewModel = hiltViewModel<HomeViewModel>()
                ScreenNavigator(viewModel) { state, effect, onEvent ->
                    HomeMobileScreen(state = state, effect = effect, onEvent = onEvent)
                }
            }
        }
}
