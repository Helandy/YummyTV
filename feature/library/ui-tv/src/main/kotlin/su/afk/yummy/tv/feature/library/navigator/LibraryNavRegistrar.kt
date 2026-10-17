package su.afk.yummy.tv.feature.library.tv.navigator

import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import su.afk.yummy.tv.core.designsystem.baseScreen.ScreenNavigator
import su.afk.yummy.tv.core.navigation.manager.INavigationManager
import su.afk.yummy.tv.feature.library.ITvLibraryEntry
import su.afk.yummy.tv.feature.library.LibraryHistoryTvScreen
import su.afk.yummy.tv.feature.library.LibraryTvScreen
import su.afk.yummy.tv.feature.library.LibraryViewModel
import su.afk.yummy.tv.feature.library.history.LibraryHistoryViewModel
import su.afk.yummy.tv.feature.library.navigator.LibraryDestination
import su.afk.yummy.tv.feature.library.navigator.LibraryHistoryDestination
import javax.inject.Inject

class LibraryNavRegistrar @Inject constructor() : ITvLibraryEntry {

    override fun register(builder: EntryProviderScope<NavKey>, nav: INavigationManager) =
        with(builder) {
            entry<LibraryDestination> { _ ->
                val viewModel = hiltViewModel<LibraryViewModel>()
                ScreenNavigator(viewModel) { state, effect, onEvent ->
                    LibraryTvScreen(state = state, effect = effect, onEvent = onEvent)
                }
            }
            entry<LibraryHistoryDestination> { _ ->
                val viewModel = hiltViewModel<LibraryHistoryViewModel>()
                ScreenNavigator(viewModel) { state, effect, onEvent ->
                    LibraryHistoryTvScreen(state = state, effect = effect, onEvent = onEvent)
                }
            }
        }
}
