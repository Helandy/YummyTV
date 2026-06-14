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
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation3.runtime.NavKey
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import su.afk.yummy.tv.core.analytics.AnalyticsDestination
import su.afk.yummy.tv.core.analytics.AnalyticsEvents
import su.afk.yummy.tv.core.analytics.AnalyticsTracker
import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.ScreenNavigator
import su.afk.yummy.tv.core.designsystem.presenter.locals.LocalPosterCardSize
import su.afk.yummy.tv.core.designsystem.presenter.locals.LocalPosterQuality
import su.afk.yummy.tv.core.designsystem.presenter.theme.YummyTvTheme
import su.afk.yummy.tv.core.navigation.AppNavHost
import su.afk.yummy.tv.core.navigation.NavRegistrar
import su.afk.yummy.tv.core.navigation.NavigationManager
import su.afk.yummy.tv.core.navigation.TvUi
import su.afk.yummy.tv.core.navigation.root.RootTab
import su.afk.yummy.tv.core.update.nav.UpdateDestination
import su.afk.yummy.tv.feature.main.api.IMainGraph
import su.afk.yummy.tv.feature.main.model.TvMenuItem
import su.afk.yummy.tv.feature.main.view.TvMainScaffold
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TvMainGraph @Inject constructor(
    private val navManager: NavigationManager,
    private val analyticsTracker: AnalyticsTracker,
    private val commonRegistrars: Set<@JvmSuppressWildcards NavRegistrar>,
    @param:TvUi private val tvRegistrars: Set<@JvmSuppressWildcards NavRegistrar>,
) : IMainGraph {

    private val menuItems = listOf(
        TvMenuItem(R.string.main_tab_search, RootTab.SEARCH, Icons.Default.Search),
        TvMenuItem(R.string.main_tab_home, RootTab.HOME, Icons.Default.Home),
        TvMenuItem(R.string.main_tab_schedule, RootTab.SCHEDULE, Icons.Default.DateRange),
        TvMenuItem(R.string.main_tab_top, RootTab.TOP, Icons.Default.Star),
        TvMenuItem(R.string.main_tab_library, RootTab.LIBRARY, Icons.AutoMirrored.Filled.List),
    )

    @Composable
    override fun MainGraph() {
        val viewModel: MainViewModel = hiltViewModel()
        val inAppFlow = navManager.appBackStack.isNotEmpty()
        val atRoot = !inAppFlow && navManager.backStack.size <= 1
        val currentDestination = navManager.backStack.lastOrNull()
        val showMainMenu = atRoot

        ScreenNavigator(viewModel) { state, effect, onEvent ->
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
                ) {
                    TvMainScaffold(
                        selectedRoot = navManager.currentRoot,
                        contentFocusKey = navManager.currentRoot to currentDestination,
                        menuItems = menuItems,
                        state = state,
                        showMainMenu = showMainMenu,
                        onEvent = onEvent,
                        toastMessage = toastMessage,
                    ) {
                        AppNavHost(
                            navManager = navManager,
                            registrars = commonRegistrars + tvRegistrars,
                            modifier = Modifier.fillMaxSize(),
                            onDestinationVisible = {
                                analyticsTracker.trackScreenView(it, surface = "tv")
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
