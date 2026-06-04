package su.afk.yummy.tv.feature.main

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.ScreenNavigator
import su.afk.yummy.tv.core.designsystem.presenter.locals.LocalPosterQuality
import su.afk.yummy.tv.core.designsystem.presenter.locals.LocalShowScreenshotsOnFocus
import su.afk.yummy.tv.core.designsystem.presenter.theme.YummyTvTheme
import su.afk.yummy.tv.core.navigation.AppNavHost
import su.afk.yummy.tv.core.navigation.MainMenuFocusTarget
import su.afk.yummy.tv.core.navigation.NavRegistrar
import su.afk.yummy.tv.core.navigation.NavigationManager
import su.afk.yummy.tv.core.navigation.TvUi
import su.afk.yummy.tv.core.navigation.tab.SideTab
import su.afk.yummy.tv.core.update.nav.UpdateDestination
import su.afk.yummy.tv.feature.account.IAccountNavigator
import su.afk.yummy.tv.feature.main.api.IMainGraph
import su.afk.yummy.tv.feature.main.view.TvMainScaffold
import su.afk.yummy.tv.feature.main.view.TvMenuItem
import su.afk.yummy.tv.feature.settings.ISettingsNavigator
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TvMainGraph @Inject constructor(
    private val navManager: NavigationManager,
    private val settingsNavigator: ISettingsNavigator,
    private val accountNavigator: IAccountNavigator,
    private val commonRegistrars: Set<@JvmSuppressWildcards NavRegistrar>,
    @param:TvUi private val tvRegistrars: Set<@JvmSuppressWildcards NavRegistrar>,
) : IMainGraph {

    private val menuItems = listOf(
        TvMenuItem(R.string.main_tab_search, SideTab.SEARCH, Icons.Default.Search),
        TvMenuItem(R.string.main_tab_home, SideTab.HOME, Icons.Default.Home),
        TvMenuItem(R.string.main_tab_schedule, SideTab.SCHEDULE, Icons.Default.DateRange),
        TvMenuItem(R.string.main_tab_top100, SideTab.TOP100, Icons.Default.Star),
        TvMenuItem(R.string.main_tab_library, SideTab.LIBRARY, Icons.AutoMirrored.Filled.List),
    )

    @Composable
    override fun MainGraph() {
        val viewModel: MainViewModel = hiltViewModel()
        val inAppFlow = navManager.appBackStack.isNotEmpty()
        val atTabRoot = !inAppFlow && navManager.backStack.size <= 1
        val settingsDestination = settingsNavigator.getSettingsDest()
        val accountDestination = accountNavigator.getAccountDest()
        val currentDestination = navManager.backStack.lastOrNull()
        val currentDestinationIsMainAction =
            currentDestination == settingsDestination || currentDestination == accountDestination
        val showMainMenu = !inAppFlow && (atTabRoot || currentDestinationIsMainAction)
        val currentMainMenuFocusTarget = when (currentDestination) {
            settingsDestination -> MainMenuFocusTarget.SETTINGS_ACTION
            accountDestination -> MainMenuFocusTarget.ACCOUNT_ACTION
            else -> MainMenuFocusTarget.SELECTED_TAB
        }

        ScreenNavigator(viewModel) { state, effect, onEvent ->
            LaunchedEffect(Unit) {
                effect.collect { eff ->
                    when (eff) {
                        is MainState.Effect.NavigateToUpdate -> navManager.navigate(
                            UpdateDestination(eff.version, eff.apkUrl, eff.changelog)
                        )
                    }
                }
            }

            LaunchedEffect(currentMainMenuFocusTarget) {
                onEvent(MainState.Event.TvRouteMenuTargetChanged(currentMainMenuFocusTarget))
            }

            LaunchedEffect(showMainMenu, navManager.pendingMainMenuFocusTarget) {
                val target = navManager.pendingMainMenuFocusTarget ?: return@LaunchedEffect
                if (!showMainMenu) return@LaunchedEffect
                onEvent(MainState.Event.TvMenuFocusRequested(target))
                navManager.clearMainMenuFocusRequest(target)
            }

            YummyTvTheme(appTheme = state.appTheme) {
                CompositionLocalProvider(
                    LocalPosterQuality provides state.posterQuality,
                    LocalShowScreenshotsOnFocus provides state.showScreenshotsOnFocus,
                ) {
                    TvMainScaffold(
                        selectedTab = navManager.currentTab,
                        contentFocusKey = navManager.currentTab to currentDestination,
                        menuItems = menuItems,
                        state = state,
                        showMainMenu = showMainMenu,
                        onEvent = onEvent,
                    ) {
                        AppNavHost(
                            navManager = navManager,
                            registrars = commonRegistrars + tvRegistrars,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }
            }
        }
    }
}
