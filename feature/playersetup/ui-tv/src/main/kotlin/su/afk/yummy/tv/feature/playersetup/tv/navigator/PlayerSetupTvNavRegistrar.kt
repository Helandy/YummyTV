package su.afk.yummy.tv.feature.playersetup.tv.navigator

import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import su.afk.yummy.tv.core.designsystem.baseScreen.ScreenNavigator
import su.afk.yummy.tv.core.navigation.manager.INavigationManager
import su.afk.yummy.tv.feature.playersetup.PlayerSetupViewModel
import su.afk.yummy.tv.feature.playersetup.navigator.ITvPlayerSetupEntry
import su.afk.yummy.tv.feature.playersetup.navigator.PlayerSetupDestination
import su.afk.yummy.tv.feature.playersetup.tv.PlayerSetupTvScreen
import javax.inject.Inject

class PlayerSetupTvNavRegistrar @Inject constructor() : ITvPlayerSetupEntry {

    override fun register(builder: EntryProviderScope<NavKey>, nav: INavigationManager) =
        with(builder) {
            entry<PlayerSetupDestination> {
                val viewModel = hiltViewModel<PlayerSetupViewModel>()

                ScreenNavigator(viewModel) { state, effect, onEvent ->
                    PlayerSetupTvScreen(state, effect, onEvent)
                }
            }
        }
}
