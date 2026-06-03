package su.afk.yummy.tv.feature.library.navigator

import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.ScreenNavigator
import su.afk.yummy.tv.core.navigation.NavRegistrar
import su.afk.yummy.tv.core.navigation.NavigationManager
import su.afk.yummy.tv.feature.library.LibraryMobileScreen
import su.afk.yummy.tv.feature.library.LibraryViewModel
import javax.inject.Inject

class LibraryNavRegistrar @Inject constructor() : NavRegistrar {
    override fun register(builder: EntryProviderScope<NavKey>, nav: NavigationManager) =
        with(builder) {
            entry<LibraryDestination> {
                val viewModel = hiltViewModel<LibraryViewModel>()
                ScreenNavigator(viewModel) { state, effect, onEvent ->
                    LibraryMobileScreen(state = state, effect = effect, onEvent = onEvent)
                }
            }
        }
}
