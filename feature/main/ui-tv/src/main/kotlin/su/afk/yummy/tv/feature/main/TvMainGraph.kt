package su.afk.yummy.tv.feature.main

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import su.afk.yummy.tv.core.designsystem.baseScreen.ScreenNavigator
import su.afk.yummy.tv.core.designsystem.components.rememberGlobalToastState
import su.afk.yummy.tv.core.designsystem.locals.LocalIsOffline
import su.afk.yummy.tv.core.designsystem.locals.LocalPosterCardSize
import su.afk.yummy.tv.core.designsystem.locals.LocalPosterQuality
import su.afk.yummy.tv.core.designsystem.theme.YummyTvTheme
import su.afk.yummy.tv.core.navigation.host.AppNavHost
import su.afk.yummy.tv.core.navigation.manager.INavigationManager
import su.afk.yummy.tv.core.network.connectivity.NetworkConnectivityMonitor
import su.afk.yummy.tv.feature.main.api.MainGraph
import su.afk.yummy.tv.feature.main.model.tvMenuItems
import su.afk.yummy.tv.feature.main.navigation.TvNavigationHolder
import su.afk.yummy.tv.feature.main.navigation.tvNavPopTransitionSpec
import su.afk.yummy.tv.feature.main.navigation.tvNavTransitionSpec
import su.afk.yummy.tv.feature.main.view.TvMainScaffold
import su.afk.yummy.tv.feature.player.navigator.PlayerDestination
import su.afk.yummy.tv.feature.update.navigator.UpdateDestination
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TvMainGraph @Inject constructor(
    private val navManager: INavigationManager,
    private val navigationHolder: TvNavigationHolder,
    private val networkConnectivityMonitor: NetworkConnectivityMonitor,
) : MainGraph {

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
            val isOnline by networkConnectivityMonitor.isOnline.collectAsStateWithLifecycle()
            val toast = rememberGlobalToastState()
            LaunchedEffect(effect) {
                effect.collect { eff ->
                    when (eff) {
                        is MainState.Effect.ShowToast -> toast.show(eff.message)
                    }
                }
            }

            YummyTvTheme(
                appTheme = state.appTheme,
                backgroundStyle = state.backgroundStyle,
                isTelevision = true,
            ) {
                CompositionLocalProvider(
                    LocalPosterQuality provides state.posterQuality,
                    LocalPosterCardSize provides state.posterCardSize,
                    LocalIsOffline provides !isOnline,
                ) {
                    TvMainScaffold(
                        selectedRoot = navManager.currentRoot,
                        contentFocusKey = navManager.currentRoot to currentDestination,
                        menuItems = tvMenuItems,
                        state = state,
                        showMainMenu = showMainMenu,
                        applyTopSafeDrawingInset = currentDestination !is PlayerDestination,
                        onEvent = onEvent,
                        toastMessage = toast.message,
                    ) {
                        AppNavHost(
                            navManager = navManager,
                            registrars = navigationHolder.registrars,
                            modifier = Modifier.fillMaxSize(),
                            transitionSpec = tvNavTransitionSpec,
                            popTransitionSpec = tvNavPopTransitionSpec,
                        )
                    }
                }
            }
        }
    }
}
