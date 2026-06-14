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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation3.runtime.NavKey
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import su.afk.yummy.tv.core.analytics.AnalyticsDestination
import su.afk.yummy.tv.core.analytics.AnalyticsEvents
import su.afk.yummy.tv.core.analytics.AnalyticsTracker
import su.afk.yummy.tv.core.analytics.analyticsParamsOf
import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.ScreenNavigator
import su.afk.yummy.tv.core.designsystem.presenter.locals.LocalPosterCardSize
import su.afk.yummy.tv.core.designsystem.presenter.locals.LocalPosterQuality
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
import su.afk.yummy.tv.feature.main.view.MobileMainScaffold
import su.afk.yummy.tv.feature.settings.ISettingsNavigator
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MobileMainGraph @Inject constructor(
    private val navManager: NavigationManager,
    private val analyticsTracker: AnalyticsTracker,
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
            var toastMessage by remember { mutableStateOf<String?>(null) }
            var toastJob by remember { mutableStateOf<Job?>(null) }
            val coroutineScope = rememberCoroutineScope()

            DisposableEffect(Unit) {
                onDispose { toastJob?.cancel() }
            }

            LaunchedEffect(Unit) {
                effect.collect { eff ->
                    when (eff) {
                        is MainState.Effect.NavigateToUpdate -> navManager.navigate(
                            UpdateDestination(eff.version, eff.apkUrl, eff.changelog)
                        )
                        is MainState.Effect.ShowToast -> {
                            toastMessage = eff.message
                            toastJob?.cancel()
                            toastJob = coroutineScope.launch {
                                delay(GLOBAL_TOAST_DURATION_MS)
                                if (toastMessage == eff.message) {
                                    toastMessage = null
                                }
                            }
                        }
                    }
                }
            }

            YummyTvTheme(appTheme = state.appTheme) {
                CompositionLocalProvider(
                    LocalPosterQuality provides state.posterQuality,
                    LocalPosterCardSize provides state.posterCardSize,
                    LocalMobileMainActions provides MobileMainActions(
                        unreadNotificationsCount = state.unreadNotificationsCount,
                        avatarUrl = if (state.isYaniSignedIn) state.yaniAvatarUrl else "",
                        onSettingsClick = {
                            analyticsTracker.trackMainAction("settings_selected")
                            navManager.navigate(settingsNavigator.getSettingsDest())
                        },
                        onAccountClick = {
                            analyticsTracker.trackMainAction("account_selected")
                            navManager.navigate(accountNavigator.getAccountDest())
                        },
                    ),
                ) {
                    MobileMainScaffold(
                        selectedDestination = navManager.currentRoot,
                        menuItems = items,
                        showBars = atTabRoot,
                        onDestinationSelected = {
                            analyticsTracker.trackMainAction(
                                action = "root_selected",
                                params = analyticsParamsOf("root" to it.name.lowercase()),
                            )
                            navManager.switchRoot(it)
                        },
                        toastMessage = toastMessage,
                    ) {
                        AppNavHost(
                            navManager = navManager,
                            registrars = commonRegistrars + mobileRegistrars,
                            modifier = Modifier.fillMaxSize(),
                            onDestinationVisible = {
                                analyticsTracker.trackScreenView(it, surface = "mobile")
                            },
                        )
                    }
                }
            }
        }
    }
}

private const val GLOBAL_TOAST_DURATION_MS = 3_000L

private fun AnalyticsTracker.trackScreenView(destination: NavKey, surface: String) {
    val analyticsDestination = destination as? AnalyticsDestination ?: return
    track(
        AnalyticsEvents.screenView(
            screenName = analyticsDestination.screenName,
            params = analyticsDestination.screenParams + (AnalyticsEvents.PARAM_SURFACE to surface),
        )
    )
}

private fun AnalyticsTracker.trackMainAction(
    action: String,
    params: Map<String, String> = emptyMap(),
) {
    track(
        AnalyticsEvents.uiAction(
            screenName = "main",
            action = action,
            params = params + (AnalyticsEvents.PARAM_SURFACE to "mobile"),
        )
    )
}
