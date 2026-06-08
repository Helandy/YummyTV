package su.afk.yummy.tv.feature.main

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.ScreenNavigator
import su.afk.yummy.tv.core.designsystem.presenter.locals.LocalPosterQuality
import su.afk.yummy.tv.core.designsystem.presenter.locals.LocalShowScreenshotsOnFocus
import su.afk.yummy.tv.core.designsystem.presenter.mobile.LocalMobileMainActions
import su.afk.yummy.tv.core.designsystem.presenter.mobile.MobileMainActions
import su.afk.yummy.tv.core.designsystem.presenter.theme.YummyTvTheme
import su.afk.yummy.tv.core.navigation.AppNavHost
import su.afk.yummy.tv.core.navigation.MobileUi
import su.afk.yummy.tv.core.navigation.NavRegistrar
import su.afk.yummy.tv.core.navigation.NavigationManager
import su.afk.yummy.tv.core.navigation.root.RootTab
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
            MobileMenuItem(
                stringResource(R.string.main_mobile_tab_search),
                RootTab.SEARCH,
                Icons.Default.Search
            ),
            MobileMenuItem(
                stringResource(R.string.main_mobile_tab_home),
                RootTab.HOME,
                Icons.Default.Home
            ),
            MobileMenuItem(
                stringResource(R.string.main_mobile_tab_schedule),
                RootTab.SCHEDULE,
                Icons.Default.DateRange
            ),
            MobileMenuItem(
                stringResource(R.string.main_mobile_tab_top),
                RootTab.TOP,
                Icons.Default.Star
            ),
            MobileMenuItem(
                stringResource(R.string.main_mobile_tab_library),
                RootTab.LIBRARY,
                Icons.AutoMirrored.Filled.List
            ),
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
                    LocalMobileMainActions provides MobileMainActions(
                        unreadNotificationsCount = state.unreadNotificationsCount,
                        avatarUrl = if (state.isYaniSignedIn) state.yaniAvatarUrl else "",
                        onSettingsClick = { navManager.navigate(settingsNavigator.getSettingsDest()) },
                        onAccountClick = { navManager.navigate(accountNavigator.getAccountDest()) },
                    ),
                ) {
                    MobileMainScaffold(
                        selectedDestination = navManager.currentRoot,
                        menuItems = items,
                        showBars = atTabRoot,
                        onDestinationSelected = { navManager.switchRoot(it) },
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

@Composable
private fun <T> MobileMainScaffold(
    selectedDestination: T,
    menuItems: List<MobileMenuItem<T>>,
    showBars: Boolean,
    onDestinationSelected: (T) -> Unit,
    content: @Composable () -> Unit,
) {
    Scaffold(
        contentWindowInsets = WindowInsets(0.dp),
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
        icon = { Icon(item.icon, contentDescription = item.label) },
    )
}
