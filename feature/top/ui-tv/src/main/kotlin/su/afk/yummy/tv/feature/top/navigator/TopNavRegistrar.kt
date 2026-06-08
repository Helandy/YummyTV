package su.afk.yummy.tv.feature.top.tv.navigator

import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.ScreenNavigator
import su.afk.yummy.tv.core.navigation.NavRegistrar
import su.afk.yummy.tv.core.navigation.NavigationManager
import su.afk.yummy.tv.feature.top.TopTvScreen
import su.afk.yummy.tv.feature.top.TopViewModel
import su.afk.yummy.tv.feature.top.navigator.TopDestination
import javax.inject.Inject

class TopNavRegistrar @Inject constructor() : NavRegistrar {

    override fun register(builder: EntryProviderScope<NavKey>, nav: NavigationManager) =
        with(builder) {
            entry<TopDestination> { _ ->
                val viewModel = hiltViewModel<TopViewModel>()
                ScreenNavigator(viewModel) { state, effect, onEvent ->
                    TopTvScreen(
                        state = state,
                        effect = effect,
                        isActiveDestination = nav.appBackStack.isEmpty() &&
                                nav.backStack.lastOrNull() == TopDestination,
                        onEvent = onEvent,
                    )
                }
            }
        }
}
