package su.afk.yummy.tv.feature.main

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CollectionsBookmark
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Newspaper
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
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
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
import kotlin.time.Duration.Companion.seconds

@Singleton
class TvMainGraph @Inject constructor(
    private val navManager: NavigationManager,
    private val commonRegistrars: Set<@JvmSuppressWildcards NavRegistrar>,
    @param:TvUi private val tvRegistrars: Set<@JvmSuppressWildcards NavRegistrar>,
) : IMainGraph {

    private val menuItems = listOf(
        TvMenuItem(R.string.main_tab_search, RootTab.SEARCH, Icons.Default.Search),
        TvMenuItem(R.string.main_tab_schedule, RootTab.SCHEDULE, Icons.Default.CalendarMonth),
        TvMenuItem(R.string.main_tab_home, RootTab.HOME, Icons.Default.Home),
        TvMenuItem(
            R.string.main_tab_collections,
            RootTab.COLLECTIONS,
            Icons.Filled.CollectionsBookmark,
        ),
        TvMenuItem(R.string.main_tab_news, RootTab.POSTS, Icons.Default.Newspaper),
        TvMenuItem(R.string.main_tab_top, RootTab.TOP, Icons.Default.Star),
        TvMenuItem(R.string.main_tab_library, RootTab.LIBRARY, Icons.AutoMirrored.Filled.List),
    )

    @Composable
    override fun MainGraph() {
        val viewModel: MainViewModel = hiltViewModel()
        val inAppFlow = navManager.appBackStack.isNotEmpty()
        val atRoot = !inAppFlow && navManager.backStack.size <= 1
        val currentDestination = navManager.backStack.lastOrNull()
        val isRequiredUpdateDestination =
            currentDestination is UpdateDestination && currentDestination.required
        val showMainMenu = atRoot && !isRequiredUpdateDestination

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
                        is MainState.Effect.NavigateToUpdate -> {
                            val destination = UpdateDestination(
                                eff.version,
                                eff.apkUrl,
                                eff.changelog,
                                required = eff.required,
                            )
                            if (eff.required) {
                                navManager.replace(destination)
                            } else {
                                navManager.navigate(destination)
                            }
                        }
                        is MainState.Effect.ShowToast -> {
                            toastMessage = eff.message
                            toastJob?.cancel()
                            toastJob = coroutineScope.launch {
                                delay(GLOBAL_TOAST_DURATION)
                                if (toastMessage == eff.message) {
                                    toastMessage = null
                                }
                            }
                        }
                    }
                }
            }

            YummyTvTheme(appTheme = state.appTheme, isTelevision = true) {
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
                            transitionSpec = {
                                fadeIn(tween(TV_NAV_TRANSITION_MILLIS)) +
                                        scaleIn(
                                            initialScale = TV_NAV_TRANSITION_SCALE,
                                            animationSpec = tween(TV_NAV_TRANSITION_MILLIS),
                                        ) togetherWith fadeOut(tween(TV_NAV_TRANSITION_MILLIS))
                            },
                            popTransitionSpec = {
                                fadeIn(tween(TV_NAV_TRANSITION_MILLIS)) togetherWith
                                        fadeOut(tween(TV_NAV_TRANSITION_MILLIS)) +
                                        scaleOut(
                                            targetScale = TV_NAV_TRANSITION_SCALE,
                                            animationSpec = tween(TV_NAV_TRANSITION_MILLIS),
                                        )
                            },
                        )
                    }
                }
            }
        }
    }
}

private val GLOBAL_TOAST_DURATION = 3.seconds
private const val TV_NAV_TRANSITION_MILLIS = 280
private const val TV_NAV_TRANSITION_SCALE = 1.05f
