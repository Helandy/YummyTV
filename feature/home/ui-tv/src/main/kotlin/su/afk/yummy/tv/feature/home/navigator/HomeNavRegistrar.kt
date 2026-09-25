package su.afk.yummy.tv.feature.home.tv.navigator

import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import su.afk.yummy.tv.core.designsystem.baseScreen.ScreenNavigator
import su.afk.yummy.tv.core.navigation.manager.INavigationManager
import su.afk.yummy.tv.feature.home.HomeTvScreen
import su.afk.yummy.tv.feature.home.HomeViewModel
import su.afk.yummy.tv.feature.home.ITvHomeEntry
import su.afk.yummy.tv.feature.home.navigator.HomeDestination
import javax.inject.Inject

class HomeNavRegistrar @Inject constructor() : ITvHomeEntry {

    override fun register(builder: EntryProviderScope<NavKey>, nav: INavigationManager) =
        with(builder) {
            entry<HomeDestination> { _ ->
                val viewModel = hiltViewModel<HomeViewModel>()
                ScreenNavigator(viewModel) { state, effect, onEvent ->
                    HomeTvScreen(state = state, effect = effect, onEvent = onEvent)
                }
            }
        }
}
