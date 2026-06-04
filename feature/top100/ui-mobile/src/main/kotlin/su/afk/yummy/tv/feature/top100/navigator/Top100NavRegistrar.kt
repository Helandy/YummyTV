package su.afk.yummy.tv.feature.top100.mobile.navigator

import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.ScreenNavigator
import su.afk.yummy.tv.core.navigation.NavRegistrar
import su.afk.yummy.tv.core.navigation.NavigationManager
import su.afk.yummy.tv.feature.top100.Top100MobileScreen
import su.afk.yummy.tv.feature.top100.Top100ViewModel
import su.afk.yummy.tv.feature.top100.navigator.Top100Destination
import javax.inject.Inject

class Top100NavRegistrar @Inject constructor() : NavRegistrar {
    override fun register(builder: EntryProviderScope<NavKey>, nav: NavigationManager) =
        with(builder) {
            entry<Top100Destination> {
                val viewModel = hiltViewModel<Top100ViewModel>()
                ScreenNavigator(viewModel) { state, effect, onEvent ->
                    Top100MobileScreen(state = state, effect = effect, onEvent = onEvent)
                }
            }
        }
}
