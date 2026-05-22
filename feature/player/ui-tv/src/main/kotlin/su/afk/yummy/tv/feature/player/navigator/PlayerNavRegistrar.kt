package su.afk.yummy.tv.feature.player.navigator

import androidx.compose.runtime.LaunchedEffect
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.ScreenNavigator
import su.afk.yummy.tv.core.navigation.NavRegistrar
import su.afk.yummy.tv.core.navigation.NavigationManager
import su.afk.yummy.tv.feature.player.PlayerTvScreen
import su.afk.yummy.tv.feature.player.PlayerViewModel
import javax.inject.Inject

class PlayerNavRegistrar @Inject constructor() : NavRegistrar {
    override fun register(builder: EntryProviderScope<NavKey>, nav: NavigationManager) =
        with(builder) {
            entry<PlayerDestination> { dest ->
                val viewModel = hiltViewModel<PlayerViewModel, PlayerViewModel.Factory>(
                    key = "player",
                    creationCallback = { factory -> factory.create(dest) },
                )
                LaunchedEffect(dest) { viewModel.loadDestination(dest) }
                ScreenNavigator(viewModel) { state, effect, onEvent ->
                    PlayerTvScreen(state = state, effect = effect, onEvent = onEvent)
                }
            }
        }
}
