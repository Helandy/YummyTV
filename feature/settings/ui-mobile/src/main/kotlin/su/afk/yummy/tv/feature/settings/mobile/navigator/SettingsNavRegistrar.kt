package su.afk.yummy.tv.feature.settings.mobile.navigator

import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import su.afk.yummy.tv.core.designsystem.baseScreen.ScreenNavigator
import su.afk.yummy.tv.core.navigation.manager.INavigationManager
import su.afk.yummy.tv.feature.settings.IMobileSettingsEntry
import su.afk.yummy.tv.feature.settings.SettingsViewModel
import su.afk.yummy.tv.feature.settings.mobile.SettingsDetailsButtonOrderMobileScreen
import su.afk.yummy.tv.feature.settings.mobile.SettingsMobileCategoryScreen
import su.afk.yummy.tv.feature.settings.mobile.SettingsMobileScreen
import su.afk.yummy.tv.feature.settings.navigator.SettingsCategoryDestination
import su.afk.yummy.tv.feature.settings.navigator.SettingsDestination
import su.afk.yummy.tv.feature.settings.navigator.SettingsDetailsButtonOrderDestination
import javax.inject.Inject

class SettingsNavRegistrar @Inject constructor() : IMobileSettingsEntry {
    override fun register(builder: EntryProviderScope<NavKey>, nav: INavigationManager) =
        with(builder) {
            entry<SettingsDestination> {
                val viewModel = hiltViewModel<SettingsViewModel>()
                ScreenNavigator(viewModel) { state, effect, onEvent ->
                    SettingsMobileScreen(
                        state = state,
                        effect = effect,
                        onEvent = onEvent,
                    )
                }
            }
            entry<SettingsCategoryDestination> { dest ->
                val viewModel = hiltViewModel<SettingsViewModel>()
                ScreenNavigator(viewModel) { state, effect, onEvent ->
                    SettingsMobileCategoryScreen(
                        category = dest.category,
                        state = state,
                        effect = effect,
                        onEvent = onEvent,
                    )
                }
            }
            entry<SettingsDetailsButtonOrderDestination> {
                val viewModel = hiltViewModel<SettingsViewModel>()
                ScreenNavigator(viewModel) { state, effect, onEvent ->
                    SettingsDetailsButtonOrderMobileScreen(
                        state = state,
                        effect = effect,
                        onEvent = onEvent,
                    )
                }
            }
        }
}
