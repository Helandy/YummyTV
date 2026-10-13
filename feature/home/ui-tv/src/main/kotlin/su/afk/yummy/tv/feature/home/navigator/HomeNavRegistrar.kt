package su.afk.yummy.tv.feature.home.tv.navigator

import su.afk.yummy.tv.feature.home.launch.ContinueWatchingLaunchViewModel
import su.afk.yummy.tv.feature.home.navigator.ContinueWatchingLaunchDestination
import su.afk.yummy.tv.feature.home.tv.launch.ContinueWatchingLaunchTvScreen
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
            entry<ContinueWatchingLaunchDestination> { dest ->
                val viewModel = hiltViewModel<ContinueWatchingLaunchViewModel, ContinueWatchingLaunchViewModel.Factory>(
                    key = "tv-continue-launch-${dest.animeId}",
                    creationCallback = { factory -> factory.create(dest.animeId) },
                )
                ScreenNavigator(viewModel) { state, effect, onEvent ->
                    ContinueWatchingLaunchTvScreen(state = state, effect = effect, onEvent = onEvent)
                }
            }
        }
}
