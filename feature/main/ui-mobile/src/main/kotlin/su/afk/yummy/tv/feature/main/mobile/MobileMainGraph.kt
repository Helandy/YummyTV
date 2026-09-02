package su.afk.yummy.tv.feature.main.mobile

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Newspaper
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import su.afk.yummy.tv.core.designsystem.baseScreen.ScreenNavigator
import su.afk.yummy.tv.core.designsystem.locals.LocalIsOffline
import su.afk.yummy.tv.core.designsystem.locals.LocalMarkNotificationPermissionRequested
import su.afk.yummy.tv.core.designsystem.locals.LocalNotificationPermissionRequested
import su.afk.yummy.tv.core.designsystem.locals.LocalPosterCardSize
import su.afk.yummy.tv.core.designsystem.locals.LocalPosterQuality
import su.afk.yummy.tv.core.designsystem.locals.LocalResolveKodikThumbnailUrl
import su.afk.yummy.tv.core.designsystem.mobile.bar.LocalMobileBottomBarUpFocusRequester
import su.afk.yummy.tv.core.designsystem.mobile.bar.LocalMobileMainActions
import su.afk.yummy.tv.core.designsystem.mobile.bar.MobileMainActions
import su.afk.yummy.tv.core.designsystem.theme.YummyTvTheme
import su.afk.yummy.tv.core.navigation.host.AppNavHost
import su.afk.yummy.tv.core.navigation.manager.INavigationManager
import su.afk.yummy.tv.core.navigation.registrar.MobileUi
import su.afk.yummy.tv.core.navigation.registrar.NavRegistrar
import su.afk.yummy.tv.core.navigation.root.RootTab
import su.afk.yummy.tv.core.network.connectivity.NetworkConnectivityMonitor
import su.afk.yummy.tv.core.preferences.settings.AppLifecycleSettingsStore
import su.afk.yummy.tv.core.utils.kodik.ResolveKodikThumbnailUrlUseCase
import su.afk.yummy.tv.feature.faq.IFaqNavigator
import su.afk.yummy.tv.feature.main.MainState
import su.afk.yummy.tv.feature.main.MainViewModel
import su.afk.yummy.tv.feature.main.api.MainGraph
import su.afk.yummy.tv.feature.main.mobile.model.MobileMenuItem
import su.afk.yummy.tv.feature.main.mobile.view.MobileMainScaffold
import su.afk.yummy.tv.feature.pages.ISitePagesNavigator
import su.afk.yummy.tv.feature.search.ISearchNavigator
import su.afk.yummy.tv.feature.settings.ISettingsNavigator
import su.afk.yummy.tv.feature.update.navigator.UpdateDestination
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.seconds

@Singleton
class MobileMainGraph @Inject internal constructor(
    private val navManager: INavigationManager,
    private val faqNavigator: IFaqNavigator,
    private val sitePagesNavigator: ISitePagesNavigator,
    private val settingsNavigator: ISettingsNavigator,
    private val searchNavigator: ISearchNavigator,
    private val commonRegistrars: Set<@JvmSuppressWildcards NavRegistrar>,
    @param:MobileUi private val mobileRegistrars: Set<@JvmSuppressWildcards NavRegistrar>,
    private val resolveKodikThumbnailUrl: ResolveKodikThumbnailUrlUseCase,
    private val appLifecycleSettingsStore: AppLifecycleSettingsStore,
    private val networkConnectivityMonitor: NetworkConnectivityMonitor,
) : MainGraph {

    @Composable
    override fun MainGraph() {
        val viewModel: MainViewModel = hiltViewModel()
        val currentDestination = navManager.backStack.lastOrNull()
        val atTabRoot = navManager.appBackStack.isEmpty() && navManager.backStack.size <= 1
        val isRequiredUpdateDestination =
            currentDestination is UpdateDestination && currentDestination.required

        ScreenNavigator(viewModel) { state, effect, _ ->
            var toastMessage by remember { mutableStateOf<String?>(null) }
            var toastJob by remember { mutableStateOf<Job?>(null) }
            val coroutineScope = rememberCoroutineScope()
            val accountSettingsFocusRequester = remember { FocusRequester() }
            val isOnline by networkConnectivityMonitor.isOnline.collectAsStateWithLifecycle()

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
                                updatesCount = eff.updatesCount,
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

            YummyTvTheme(
                appTheme = state.appTheme,
                backgroundStyle = state.backgroundStyle,
                isTelevision = false,
            ) {
                val items = listOf(
                    MobileMenuItem(
                        stringResource(R.string.main_mobile_tab_news),
                        RootTab.POSTS,
                        Icons.Default.Newspaper
                    ),
                    MobileMenuItem(
                        stringResource(R.string.main_mobile_tab_top),
                        RootTab.TOP,
                        Icons.Default.Star
                    ),
                    MobileMenuItem(
                        stringResource(R.string.main_mobile_tab_home),
                        RootTab.HOME,
                        Icons.Default.Home
                    ),
                    MobileMenuItem(
                        stringResource(R.string.main_mobile_tab_library),
                        RootTab.LIBRARY,
                        Icons.AutoMirrored.Filled.List
                    ),
                    MobileMenuItem(
                        stringResource(R.string.main_mobile_tab_profile),
                        RootTab.ACCOUNT,
                        Icons.Default.AccountCircle,
                        badgeCount = state.unreadNotificationsCount,
                    ),
                )

                CompositionLocalProvider(
                    LocalPosterQuality provides state.posterQuality,
                    LocalPosterCardSize provides state.posterCardSize,
                    LocalIsOffline provides !isOnline,
                    LocalResolveKodikThumbnailUrl provides resolveKodikThumbnailUrl::invoke,
                    LocalNotificationPermissionRequested provides
                            appLifecycleSettingsStore.notificationPermissionRequested,
                    LocalMarkNotificationPermissionRequested provides
                            appLifecycleSettingsStore::markNotificationPermissionRequested,
                    LocalMobileBottomBarUpFocusRequester provides accountSettingsFocusRequester.takeIf {
                        atTabRoot && navManager.currentRoot == RootTab.ACCOUNT
                    },
                    LocalMobileMainActions provides MobileMainActions(
                        onFaqClick = {
                            navManager.navigate(faqNavigator.getFaqDest())
                        },
                        onSitePagesClick = {
                            navManager.navigate(sitePagesNavigator.pages())
                        },
                        onSettingsClick = {
                            navManager.navigate(settingsNavigator.getSettingsDest())
                        },
                        onSearchClick = {
                            navManager.navigate(searchNavigator.getSearchDest())
                        },
                    ),
                ) {
                    MobileMainScaffold(
                        selectedDestination = navManager.currentRoot,
                        menuItems = items,
                        showBars = atTabRoot && !isRequiredUpdateDestination,
                        onDestinationSelected = navManager::switchRoot,
                        toastMessage = toastMessage,
                    ) {
                        AppNavHost(
                            navManager = navManager,
                            registrars = commonRegistrars + mobileRegistrars,
                            modifier = Modifier.fillMaxSize(),
                            transitionSpec = {
                                fadeIn(
                                    tween(
                                        durationMillis = MOBILE_NAV_FADE_IN_MILLIS,
                                        delayMillis = MOBILE_NAV_FADE_IN_DELAY_MILLIS,
                                    ),
                                ) + scaleIn(
                                    initialScale = MOBILE_NAV_TRANSITION_SCALE,
                                    animationSpec = tween(MOBILE_NAV_TRANSITION_MILLIS),
                                ) togetherWith fadeOut(tween(MOBILE_NAV_FADE_OUT_MILLIS))
                            },
                            popTransitionSpec = {
                                fadeIn(
                                    tween(
                                        durationMillis = MOBILE_NAV_FADE_IN_MILLIS,
                                        delayMillis = MOBILE_NAV_FADE_IN_DELAY_MILLIS,
                                    ),
                                ) togetherWith fadeOut(tween(MOBILE_NAV_FADE_OUT_MILLIS)) +
                                        scaleOut(
                                            targetScale = MOBILE_NAV_TRANSITION_SCALE,
                                            animationSpec = tween(MOBILE_NAV_TRANSITION_MILLIS),
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

private const val MOBILE_NAV_TRANSITION_MILLIS = 300
private const val MOBILE_NAV_FADE_IN_MILLIS = 220
private const val MOBILE_NAV_FADE_IN_DELAY_MILLIS = 60
private const val MOBILE_NAV_FADE_OUT_MILLIS = 120
private const val MOBILE_NAV_TRANSITION_SCALE = 0.94f
