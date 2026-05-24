package su.afk.yummy.tv.feature.settings.navigator

import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.ScreenNavigator
import su.afk.yummy.tv.core.navigation.NavRegistrar
import su.afk.yummy.tv.core.navigation.NavigationManager
import su.afk.yummy.tv.feature.settings.SettingsTvScreen
import su.afk.yummy.tv.feature.settings.SettingsViewModel
import javax.inject.Inject

class SettingsNavRegistrar @Inject constructor() : NavRegistrar {
    override fun register(builder: EntryProviderScope<NavKey>, nav: NavigationManager) =
        with(builder) {
            entry<SettingsDestination> {
                val viewModel = hiltViewModel<SettingsViewModel>()
                ScreenNavigator(viewModel) { state, effect, onEvent ->
                    SettingsTvScreen(state = state, effect = effect, onEvent = onEvent)
                }
            }
        }
}
