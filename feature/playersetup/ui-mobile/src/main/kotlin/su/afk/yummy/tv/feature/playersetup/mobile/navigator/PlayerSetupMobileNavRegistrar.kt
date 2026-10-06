package su.afk.yummy.tv.feature.playersetup.mobile.navigator

import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import su.afk.yummy.tv.core.designsystem.baseScreen.ScreenNavigator
import su.afk.yummy.tv.core.navigation.manager.INavigationManager
import su.afk.yummy.tv.feature.playersetup.PlayerSetupViewModel
import su.afk.yummy.tv.feature.playersetup.mobile.PlayerSetupMobileScreen
import su.afk.yummy.tv.feature.playersetup.navigator.IMobilePlayerSetupEntry
import su.afk.yummy.tv.feature.playersetup.navigator.PlayerSetupDestination
import javax.inject.Inject

class PlayerSetupMobileNavRegistrar @Inject constructor() : IMobilePlayerSetupEntry {

    override fun register(builder: EntryProviderScope<NavKey>, nav: INavigationManager) =
        with(builder) {
            entry<PlayerSetupDestination> {
                val viewModel = hiltViewModel<PlayerSetupViewModel>()

                ScreenNavigator(viewModel) { state, effect, onEvent ->
                    PlayerSetupMobileScreen(state, effect, onEvent)
                }
            }
        }
}
