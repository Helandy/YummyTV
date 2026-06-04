package su.afk.yummy.tv.feature.main

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.ScreenNavigator
import su.afk.yummy.tv.core.designsystem.presenter.locals.LocalPosterQuality
import su.afk.yummy.tv.core.designsystem.presenter.locals.LocalShowScreenshotsOnFocus
import su.afk.yummy.tv.core.designsystem.presenter.theme.YummyTvTheme
import su.afk.yummy.tv.core.navigation.AppNavHost
import su.afk.yummy.tv.core.navigation.MobileUi
import su.afk.yummy.tv.core.navigation.NavRegistrar
import su.afk.yummy.tv.core.navigation.NavigationManager
import su.afk.yummy.tv.core.navigation.tab.SideTab
import su.afk.yummy.tv.core.update.nav.UpdateDestination
import su.afk.yummy.tv.feature.account.IAccountNavigator
import su.afk.yummy.tv.feature.main.api.IMainGraph
import su.afk.yummy.tv.feature.main.mobile.R
import su.afk.yummy.tv.feature.main.model.MobileMenuItem
import su.afk.yummy.tv.feature.settings.ISettingsNavigator
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MobileMainGraph @Inject constructor(
    private val navManager: NavigationManager,
    private val settingsNavigator: ISettingsNavigator,
    private val accountNavigator: IAccountNavigator,
    private val commonRegistrars: Set<@JvmSuppressWildcards NavRegistrar>,
    @param:MobileUi private val mobileRegistrars: Set<@JvmSuppressWildcards NavRegistrar>,
) : IMainGraph {
    @Composable
    override fun MainGraph() {
        val viewModel: MainViewModel = hiltViewModel()
        val items = listOf(
            MobileMenuItem(stringResource(R.string.main_mobile_tab_search), SideTab.SEARCH, Icons.Default.Search),
            MobileMenuItem(stringResource(R.string.main_mobile_tab_home), SideTab.HOME, Icons.Default.Home),
            MobileMenuItem(stringResource(R.string.main_mobile_tab_schedule), SideTab.SCHEDULE, Icons.Default.DateRange),
            MobileMenuItem(stringResource(R.string.main_mobile_tab_top), SideTab.TOP100, Icons.Default.Star),
            MobileMenuItem(stringResource(R.string.main_mobile_tab_library), SideTab.LIBRARY, Icons.AutoMirrored.Filled.List),
        )
        val atTabRoot = navManager.appBackStack.isEmpty() && navManager.backStack.size <= 1

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

            YummyTvTheme(appTheme = state.appTheme) {
                CompositionLocalProvider(
                    LocalPosterQuality provides state.posterQuality,
                    LocalShowScreenshotsOnFocus provides false,
                ) {
                    MobileMainScaffold(
                        selectedDestination = navManager.currentTab,
                        menuItems = items,
                        showBars = atTabRoot,
                        unreadNotificationsCount = state.unreadNotificationsCount,
                        onDestinationSelected = { navManager.switchTab(it) },
                        onSettingsClick = { navManager.navigate(settingsNavigator.getSettingsDest()) },
                        onAccountClick = { navManager.navigate(accountNavigator.getAccountDest()) },
                    ) {
                        AppNavHost(
                            navManager = navManager,
                            registrars = commonRegistrars + mobileRegistrars,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T> MobileMainScaffold(
    selectedDestination: T,
    menuItems: List<MobileMenuItem<T>>,
    showBars: Boolean,
    unreadNotificationsCount: Int,
    onDestinationSelected: (T) -> Unit,
    onSettingsClick: () -> Unit,
    onAccountClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    Scaffold(
        topBar = {
            if (showBars) {
                TopAppBar(
                    title = { Text("YummyTV") },
                    actions = {
                        IconButton(onClick = onSettingsClick) {
                            Icon(Icons.Default.Settings, contentDescription = null)
                        }
                        IconButton(onClick = onAccountClick) {
                            BadgedBox(
                                badge = {
                                    if (unreadNotificationsCount > 0) {
                                        Badge { Text(unreadNotificationsCount.toString()) }
                                    }
                                },
                            ) {
                                Icon(Icons.Default.AccountCircle, contentDescription = null)
                            }
                        }
                    },
                )
            }
        },
        bottomBar = {
            if (showBars) {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                    menuItems.forEach { item ->
                        MobileNavigationItem(
                            item = item,
                            selected = item.destination == selectedDestination,
                            onSelected = { onDestinationSelected(item.destination) },
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            content()
        }
    }
}

@Composable
private fun <T> RowScope.MobileNavigationItem(
    item: MobileMenuItem<T>,
    selected: Boolean,
    onSelected: () -> Unit,
) {
    NavigationBarItem(
        selected = selected,
        onClick = onSelected,
        icon = { Icon(item.icon, contentDescription = null) },
        label = { Text(item.label, maxLines = 1) },
    )
}
