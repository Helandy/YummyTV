package su.afk.yummy.tv.feature.library.mobile.navigator

import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import su.afk.yummy.tv.core.designsystem.baseScreen.ScreenNavigator
import su.afk.yummy.tv.core.navigation.manager.INavigationManager
import su.afk.yummy.tv.feature.library.IMobileLibraryEntry
import su.afk.yummy.tv.feature.library.LibraryViewModel
import su.afk.yummy.tv.feature.library.mobile.LibraryMobileScreen
import su.afk.yummy.tv.feature.library.mobile.transfer.ListTransferMobileScreen
import su.afk.yummy.tv.feature.library.navigator.LibraryDestination
import su.afk.yummy.tv.feature.library.navigator.LibraryListTransferDestination
import su.afk.yummy.tv.feature.library.transfer.ListTransferViewModel
import javax.inject.Inject

class LibraryNavRegistrar @Inject constructor() : IMobileLibraryEntry {
    override fun register(builder: EntryProviderScope<NavKey>, nav: INavigationManager) =
        with(builder) {
            entry<LibraryDestination> {
                val viewModel = hiltViewModel<LibraryViewModel>()
                ScreenNavigator(viewModel) { state, effect, onEvent ->
                    LibraryMobileScreen(state = state, effect = effect, onEvent = onEvent)
                }
            }
            entry<LibraryListTransferDestination> {
                val viewModel = hiltViewModel<ListTransferViewModel>()
                ScreenNavigator(viewModel) { state, effect, onEvent ->
                    ListTransferMobileScreen(state = state, effect = effect, onEvent = onEvent)
                }
            }
        }
}
