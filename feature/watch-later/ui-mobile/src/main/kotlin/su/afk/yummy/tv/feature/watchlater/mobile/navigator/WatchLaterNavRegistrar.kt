package su.afk.yummy.tv.feature.watchlater.mobile.navigator

import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import su.afk.yummy.tv.core.designsystem.baseScreen.ScreenNavigator
import su.afk.yummy.tv.core.navigation.manager.INavigationManager
import su.afk.yummy.tv.feature.watchlater.IMobileWatchLaterEntry
import su.afk.yummy.tv.feature.watchlater.WatchLaterViewModel
import su.afk.yummy.tv.feature.watchlater.mobile.WatchLaterMobileScreen
import su.afk.yummy.tv.feature.watchlater.navigator.WatchLaterDestination
import javax.inject.Inject

class WatchLaterNavRegistrar @Inject constructor() : IMobileWatchLaterEntry {
    override fun register(builder: EntryProviderScope<NavKey>, nav: INavigationManager) =
        with(builder) {
            entry<WatchLaterDestination> {
                val viewModel = hiltViewModel<WatchLaterViewModel>()
                ScreenNavigator(viewModel) { state, effect, onEvent ->
                    WatchLaterMobileScreen(state = state, effect = effect, onEvent = onEvent)
                }
            }
        }
}
