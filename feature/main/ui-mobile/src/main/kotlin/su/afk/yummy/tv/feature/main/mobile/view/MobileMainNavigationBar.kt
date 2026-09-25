package su.afk.yummy.tv.feature.main.mobile.view

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.core.designsystem.mobile.input.clickablePointer
import su.afk.yummy.tv.core.designsystem.mobile.bar.MobileBottomBarDefaults
import su.afk.yummy.tv.feature.main.mobile.model.MobileMenuItem

@Composable
internal fun <T> MobileMainNavigationBar(
    selectedDestination: T,
    menuItems: List<MobileMenuItem<T>>,
    containerColor: Color,
    upFocusRequester: FocusRequester?,
    onDestinationSelected: (T) -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(containerColor)
            .navigationBarsPadding(),
    ) {
        NavigationBar(
            modifier = Modifier.height(MobileBottomBarDefaults.BarHeight),
            containerColor = containerColor,
            windowInsets = WindowInsets(0.dp),
        ) {
            menuItems.forEach { item ->
                val selected = item.destination == selectedDestination
                NavigationBarItem(
                    modifier = Modifier
                        .testTag("main_tab")
                        .clickablePointer()
                        .focusProperties {
                            upFocusRequester?.takeIf { selected }?.let { up = it }
                        },
                    selected = selected,
                    onClick = { onDestinationSelected(item.destination) },
                    icon = { MobileMainNavigationIcon(item) },
                )
            }
        }
    }
}
