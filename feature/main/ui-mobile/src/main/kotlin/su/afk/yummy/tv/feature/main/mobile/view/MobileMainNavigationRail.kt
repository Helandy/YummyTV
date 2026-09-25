package su.afk.yummy.tv.feature.main.mobile.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.core.designsystem.mobile.input.clickablePointer
import su.afk.yummy.tv.feature.main.mobile.model.MobileMenuItem

/** Рейка для широких окон: те же вкладки, что в нижнем баре, по центру по вертикали. */
@Composable
internal fun <T> MobileMainNavigationRail(
    selectedDestination: T,
    menuItems: List<MobileMenuItem<T>>,
    containerColor: Color,
    onDestinationSelected: (T) -> Unit,
) {
    NavigationRail(
        modifier = Modifier.fillMaxHeight(),
        containerColor = containerColor,
    ) {
        Column(
            modifier = Modifier.fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(RailItemSpacing, Alignment.CenterVertically),
        ) {
            menuItems.forEach { item ->
                NavigationRailItem(
                    modifier = Modifier
                        .testTag("main_tab")
                        .clickablePointer(),
                    selected = item.destination == selectedDestination,
                    onClick = { onDestinationSelected(item.destination) },
                    icon = { MobileMainNavigationIcon(item) },
                )
            }
        }
    }
}

private val RailItemSpacing = 12.dp
