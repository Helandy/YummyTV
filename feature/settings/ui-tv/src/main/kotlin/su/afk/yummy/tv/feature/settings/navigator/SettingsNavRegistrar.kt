package su.afk.yummy.tv.feature.settings.tv.navigator

import androidx.compose.runtime.DisposableEffect
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.ScreenNavigator
import su.afk.yummy.tv.core.navigation.MainMenuFocusTarget
import su.afk.yummy.tv.core.navigation.NavRegistrar
import su.afk.yummy.tv.core.navigation.NavigationManager
import su.afk.yummy.tv.feature.settings.SettingsTvScreen
import su.afk.yummy.tv.feature.settings.SettingsViewModel
import su.afk.yummy.tv.feature.settings.navigator.SettingsDestination
import javax.inject.Inject

class SettingsNavRegistrar @Inject constructor() : NavRegistrar {
    override fun register(builder: EntryProviderScope<NavKey>, nav: NavigationManager) =
        with(builder) {
            entry<SettingsDestination> {
                val viewModel = hiltViewModel<SettingsViewModel>()
                DisposableEffect(Unit) {
                    onDispose {
                        nav.requestMainMenuFocus(MainMenuFocusTarget.SETTINGS_ACTION)
                    }
                }
                ScreenNavigator(viewModel) { state, effect, onEvent ->
                    SettingsTvScreen(state = state, effect = effect, onEvent = onEvent)
                }
            }
        }
}
