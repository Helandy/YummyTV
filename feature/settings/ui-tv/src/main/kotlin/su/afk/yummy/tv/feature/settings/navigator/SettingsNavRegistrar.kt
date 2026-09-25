package su.afk.yummy.tv.feature.settings.tv.navigator

import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import su.afk.yummy.tv.core.designsystem.baseScreen.ScreenNavigator
import su.afk.yummy.tv.core.navigation.manager.INavigationManager
import su.afk.yummy.tv.feature.settings.ITvSettingsEntry
import su.afk.yummy.tv.feature.settings.SettingsTvScreen
import su.afk.yummy.tv.feature.settings.SettingsViewModel
import su.afk.yummy.tv.feature.settings.navigator.SettingsDestination
import javax.inject.Inject

class SettingsNavRegistrar @Inject constructor() : ITvSettingsEntry {
    override fun register(builder: EntryProviderScope<NavKey>, nav: INavigationManager) =
        with(builder) {
            entry<SettingsDestination> {
                val viewModel = hiltViewModel<SettingsViewModel>()
                ScreenNavigator(viewModel) { state, effect, onEvent ->
                    SettingsTvScreen(state = state, effect = effect, onEvent = onEvent)
                }
            }
        }
}
