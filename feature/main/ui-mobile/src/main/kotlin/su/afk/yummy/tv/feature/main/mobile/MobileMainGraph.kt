package su.afk.yummy.tv.feature.main.mobile

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import su.afk.yummy.tv.core.designsystem.baseScreen.ScreenNavigator
import su.afk.yummy.tv.core.designsystem.components.rememberGlobalToastState
import su.afk.yummy.tv.core.designsystem.locals.LocalIsOffline
import su.afk.yummy.tv.core.designsystem.locals.LocalPosterCardSize
import su.afk.yummy.tv.core.designsystem.locals.LocalPosterQuality
import su.afk.yummy.tv.core.designsystem.locals.LocalResolveKodikThumbnailUrl
import su.afk.yummy.tv.core.designsystem.mobile.bar.LocalMobileBottomBarUpFocusRequester
import su.afk.yummy.tv.core.designsystem.theme.YummyTvTheme
import su.afk.yummy.tv.core.navigation.host.AppNavHost
import su.afk.yummy.tv.core.navigation.manager.INavigationManager
import su.afk.yummy.tv.core.navigation.root.RootTab
import su.afk.yummy.tv.core.navigation.scene.FullscreenDestination
import su.afk.yummy.tv.core.network.connectivity.NetworkConnectivityMonitor
import su.afk.yummy.tv.core.utils.kodik.ResolveKodikThumbnailUrlUseCase
import su.afk.yummy.tv.feature.main.MainState
import su.afk.yummy.tv.feature.main.MainViewModel
import su.afk.yummy.tv.feature.main.api.MainGraph
import su.afk.yummy.tv.feature.main.mobile.model.mobileMenuItems
import su.afk.yummy.tv.feature.main.mobile.navigation.MobileNavigationHolder
import su.afk.yummy.tv.feature.main.mobile.navigation.mobileNavPopTransitionSpec
import su.afk.yummy.tv.feature.main.mobile.navigation.mobileNavTransitionSpec
import su.afk.yummy.tv.feature.main.mobile.navigation.rememberMobileListDetailSceneStrategy
import su.afk.yummy.tv.feature.main.mobile.view.MobileMainScaffold
import su.afk.yummy.tv.feature.update.navigator.UpdateDestination
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MobileMainGraph @Inject internal constructor(
    private val navManager: INavigationManager,
    private val navigationHolder: MobileNavigationHolder,
    private val resolveKodikThumbnailUrl: ResolveKodikThumbnailUrlUseCase,
    private val networkConnectivityMonitor: NetworkConnectivityMonitor,
) : MainGraph {

    @Composable
    override fun MainGraph() {
        val viewModel: MainViewModel = hiltViewModel()
        val currentDestination = navManager.backStack.lastOrNull()
        val atTabRoot = navManager.appBackStack.isEmpty() && navManager.backStack.size <= 1
        val isRequiredUpdateDestination =
            currentDestination is UpdateDestination && currentDestination.required
        // Рейка на широком окне остаётся рядом с деталями, уходит только в полноэкранных сценах.
        val showRail = !isRequiredUpdateDestination && currentDestination !is FullscreenDestination

        ScreenNavigator(viewModel) { state, effect, onEvent ->
            val accountSettingsFocusRequester = remember { FocusRequester() }
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
                isTelevision = false,
            ) {
                CompositionLocalProvider(
                    LocalPosterQuality provides state.posterQuality,
                    LocalPosterCardSize provides state.posterCardSize,
                    LocalIsOffline provides !isOnline,
                    LocalResolveKodikThumbnailUrl provides resolveKodikThumbnailUrl::invoke,
                    LocalMobileBottomBarUpFocusRequester provides accountSettingsFocusRequester.takeIf {
                        atTabRoot && navManager.currentRoot == RootTab.ACCOUNT
                    },
                ) {
                    MobileMainScaffold(
                        selectedDestination = navManager.currentRoot,
                        menuItems = mobileMenuItems(state.unreadNotificationsCount),
                        showBottomBar = atTabRoot && !isRequiredUpdateDestination,
                        showRail = showRail,
                        onDestinationSelected = { root ->
                            onEvent(MainState.Event.RootSelected(root, popToRootOnReselect = true))
                        },
                        toastMessage = toast.message,
                    ) {
                        AppNavHost(
                            navManager = navManager,
                            registrars = navigationHolder.registrars,
                            modifier = Modifier.fillMaxSize(),
                            extraSceneStrategies = listOf(
                                rememberMobileListDetailSceneStrategy(currentDestination),
                            ),
                            transitionSpec = mobileNavTransitionSpec,
                            popTransitionSpec = mobileNavPopTransitionSpec,
                        )
                    }
                }
            }
        }
    }
}
