package su.afk.yummy.tv.feature.main

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.ScreenNavigator
import su.afk.yummy.tv.core.designsystem.presenter.locals.LocalPosterQuality
import su.afk.yummy.tv.core.designsystem.presenter.locals.LocalShowScreenshotsOnFocus
import su.afk.yummy.tv.core.designsystem.presenter.theme.YummyTvTheme
import su.afk.yummy.tv.core.navigation.AppNavHost
import su.afk.yummy.tv.core.navigation.NavRegistrar
import su.afk.yummy.tv.core.navigation.NavigationManager
import su.afk.yummy.tv.core.navigation.tab.SideTab
import su.afk.yummy.tv.core.update.nav.UpdateDestination
import su.afk.yummy.tv.feature.main.api.ITvMainGraph
import su.afk.yummy.tv.feature.main.view.TvMainScaffold
import su.afk.yummy.tv.feature.main.view.TvMenuItem
import su.afk.yummy.tv.feature.settings.ISettingsNavigator
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TvMainGraph @Inject constructor(
    private val navManager: NavigationManager,
    private val settingsNavigator: ISettingsNavigator,
    private val registrars: Set<@JvmSuppressWildcards NavRegistrar>,
) : ITvMainGraph {

    private val menuItems = listOf(
        TvMenuItem(R.string.main_tab_search, SideTab.SEARCH, Icons.Default.Search),
        TvMenuItem(R.string.main_tab_home, SideTab.HOME, Icons.Default.Home),
        TvMenuItem(R.string.main_tab_top100, SideTab.TOP100, Icons.Default.Star),
        TvMenuItem(R.string.main_tab_library, SideTab.LIBRARY, Icons.AutoMirrored.Filled.List),
    )

    @Composable
    override fun MainGraph() {
        val viewModel: MainViewModel = hiltViewModel()
        var savedTab by rememberSaveable { mutableStateOf(navManager.currentTab) }
        var restoredTab by remember { mutableStateOf(false) }
        val atTabRoot = navManager.backStack.size <= 1

        LaunchedEffect(Unit) {
            navManager.restoreTab(savedTab)
            restoredTab = true
        }

        LaunchedEffect(navManager.currentTab, restoredTab) {
            if (restoredTab) savedTab = navManager.currentTab
        }

        ScreenNavigator(viewModel) { state, effect, _ ->
            LaunchedEffect(Unit) {
                effect.collect { eff ->
                    when (eff) {
                        is MainState.Effect.NavigateToUpdate -> navManager.navigate(
                            UpdateDestination(eff.version, eff.apkUrl, eff.changelog)
                        )
                    }
                }
            }

            YummyTvTheme {
                CompositionLocalProvider(
                    LocalPosterQuality provides state.posterQuality,
                    LocalShowScreenshotsOnFocus provides state.showScreenshotsOnFocus,
                ) {
                    TvMainScaffold(
                        selectedDestination = navManager.currentTab,
                        menuItems = menuItems,
                        onDestinationSelected = { navManager.switchTab(it) },
                        onSettingsClick = { navManager.navigate(settingsNavigator.getSettingsDest()) },
                        showTopBar = atTabRoot,
                    ) {
                        AppNavHost(
                            navManager = navManager,
                            registrars = registrars,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }
            }
        }
    }
}
