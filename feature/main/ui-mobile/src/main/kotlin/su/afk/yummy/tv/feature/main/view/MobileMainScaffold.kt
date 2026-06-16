package su.afk.yummy.tv.feature.main.view

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
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
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.core.designsystem.presenter.components.GlobalToastOverlay
import su.afk.yummy.tv.feature.main.model.MobileMenuItem

@Composable
internal fun <T> MobileMainScaffold(
    selectedDestination: T,
    menuItems: List<MobileMenuItem<T>>,
    showBars: Boolean,
    onDestinationSelected: (T) -> Unit,
    toastMessage: String?,
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
            GlobalToastOverlay(text = toastMessage)
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
