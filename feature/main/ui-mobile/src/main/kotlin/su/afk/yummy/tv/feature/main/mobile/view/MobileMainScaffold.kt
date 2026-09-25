package su.afk.yummy.tv.feature.main.mobile.view

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffoldDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffoldLayout
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.material3.adaptive.navigationsuite.rememberNavigationSuiteScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import su.afk.yummy.tv.core.designsystem.components.GlobalToastOverlay
import su.afk.yummy.tv.core.designsystem.mobile.bar.LocalMobileBottomBarUpFocusRequester
import su.afk.yummy.tv.core.designsystem.mobile.bar.LocalMobileNavigationLayout
import su.afk.yummy.tv.core.designsystem.mobile.bar.MobileBottomBarDefaults
import su.afk.yummy.tv.core.designsystem.mobile.bar.MobileNavigationLayout
import su.afk.yummy.tv.feature.main.mobile.model.MobileMenuItem

/**
 * Корневой каркас мобильного UI: на компактной ширине — нижний бар, на планшете, развёрнутом
 * складном и в широком окне — боковая рейка. [showBottomBar] и [showRail] задаются раздельно:
 * бар занимает высоту контента и уходит вне корня таба, рейка остаётся рядом с деталями.
 */
@Composable
internal fun <T> MobileMainScaffold(
    selectedDestination: T,
    menuItems: List<MobileMenuItem<T>>,
    showBottomBar: Boolean,
    showRail: Boolean,
    onDestinationSelected: (T) -> Unit,
    toastMessage: String?,
    content: @Composable () -> Unit,
) {
    val bottomBarUpFocusRequester = LocalMobileBottomBarUpFocusRequester.current
    val suiteType = NavigationSuiteScaffoldDefaults.navigationSuiteType(currentWindowAdaptiveInfoV2())
    val isRail = suiteType != NavigationSuiteType.NavigationBar &&
        suiteType != NavigationSuiteType.ShortNavigationBarCompact &&
        suiteType != NavigationSuiteType.ShortNavigationBarMedium
    val isVisible = if (isRail) showRail else showBottomBar
    val layout = when {
        !isVisible -> MobileNavigationLayout.Hidden
        isRail -> MobileNavigationLayout.Rail
        else -> MobileNavigationLayout.BottomBar
    }

    val suiteState = rememberNavigationSuiteScaffoldState()
    LaunchedEffect(isVisible) {
        if (isVisible) suiteState.show() else suiteState.hide()
    }

    val surface = MaterialTheme.colorScheme.surface
    NavigationSuiteScaffoldLayout(
        navigationSuiteType = if (isRail) {
            NavigationSuiteType.NavigationRail
        } else {
            NavigationSuiteType.NavigationBar
        },
        state = suiteState,
        navigationSuite = {
            if (isRail) {
                MobileMainNavigationRail(
                    selectedDestination = selectedDestination,
                    menuItems = menuItems,
                    containerColor = surface,
                    onDestinationSelected = onDestinationSelected,
                )
            } else {
                MobileMainNavigationBar(
                    selectedDestination = selectedDestination,
                    menuItems = menuItems,
                    containerColor = surface,
                    upFocusRequester = bottomBarUpFocusRequester,
                    onDestinationSelected = onDestinationSelected,
                )
            }
        },
    ) {
        CompositionLocalProvider(LocalMobileNavigationLayout provides layout) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background),
            ) {
                content()
                GlobalToastOverlay(
                    text = toastMessage,
                    modifier = Modifier.padding(bottom = MobileBottomBarDefaults.contentBottomPadding),
                )
            }
        }
    }
}
