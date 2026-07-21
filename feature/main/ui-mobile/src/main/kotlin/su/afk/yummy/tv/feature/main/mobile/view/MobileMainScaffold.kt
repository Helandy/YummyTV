package su.afk.yummy.tv.feature.main.mobile.view

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.core.designsystem.presenter.components.GlobalToastOverlay
import su.afk.yummy.tv.core.designsystem.presenter.mobile.LocalMobileBottomBarUpFocusRequester
import su.afk.yummy.tv.feature.main.mobile.model.MobileMenuItem

@Composable
internal fun <T> MobileMainScaffold(
    selectedDestination: T,
    menuItems: List<MobileMenuItem<T>>,
    showBars: Boolean,
    onDestinationSelected: (T) -> Unit,
    toastMessage: String?,
    content: @Composable () -> Unit,
) {
    val bottomBarUpFocusRequester = LocalMobileBottomBarUpFocusRequester.current
    Scaffold(
        contentWindowInsets = WindowInsets(0.dp),
        bottomBar = {
            AnimatedVisibility(
                visible = showBars,
                enter = slideInVertically(tween(BOTTOM_BAR_ANIMATION_MILLIS)) { it } +
                        fadeIn(tween(BOTTOM_BAR_ANIMATION_MILLIS)),
                exit = slideOutVertically(tween(BOTTOM_BAR_ANIMATION_MILLIS)) { it } +
                        fadeOut(tween(BOTTOM_BAR_ANIMATION_MILLIS)),
            ) {
                val surface = MaterialTheme.colorScheme.surface
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(surface)
                        .navigationBarsPadding(),
                ) {
                    NavigationBar(
                        containerColor = surface,
                        windowInsets = WindowInsets(0.dp),
                    ) {
                        menuItems.forEach { item ->
                            MobileNavigationItem(
                                item = item,
                                selected = item.destination == selectedDestination,
                                upFocusRequester = bottomBarUpFocusRequester.takeIf {
                                    item.destination == selectedDestination
                                },
                                onSelected = { onDestinationSelected(item.destination) },
                            )
                        }
                    }
                }
            }
        },
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            content()
            GlobalToastOverlay(
                text = toastMessage,
                modifier = Modifier.padding(bottom = innerPadding.calculateBottomPadding()),
            )
        }
    }
}

private const val BOTTOM_BAR_ANIMATION_MILLIS = 220

@Composable
private fun <T> RowScope.MobileNavigationItem(
    item: MobileMenuItem<T>,
    selected: Boolean,
    upFocusRequester: FocusRequester?,
    onSelected: () -> Unit,
) {
    NavigationBarItem(
        modifier = Modifier.focusProperties {
            upFocusRequester?.let { up = it }
        },
        selected = selected,
        onClick = onSelected,
        icon = {
            BadgedBox(
                badge = {
                    if (item.badgeCount > 0) {
                        Badge { Text(item.badgeCount.toString()) }
                    }
                },
            ) {
                Icon(item.icon, contentDescription = item.label)
            }
        },
    )
}
