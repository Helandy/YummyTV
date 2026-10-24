package su.afk.yummy.tv.core.designsystem.seasonal

import androidx.compose.material3.NavigationBarItemColors
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRailItemColors
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import su.afk.yummy.tv.core.model.seasonal.SeasonalTheme

private val HalloweenIndicator = Color(0x66FF7A1A)

/** Цвета пунктов нижнего бара: на Хэллоуин индикатор активной вкладки становится тыквенным. */
@Composable
fun seasonalNavigationBarItemColors(): NavigationBarItemColors = when (rememberSeasonalTheme()) {
    SeasonalTheme.HALLOWEEN -> NavigationBarItemDefaults.colors(indicatorColor = HalloweenIndicator)
    SeasonalTheme.NEW_YEAR, SeasonalTheme.NONE -> NavigationBarItemDefaults.colors()
}

/** Цвета пунктов боковой рейки: на Хэллоуин индикатор активной вкладки становится тыквенным. */
@Composable
fun seasonalNavigationRailItemColors(): NavigationRailItemColors = when (rememberSeasonalTheme()) {
    SeasonalTheme.HALLOWEEN -> NavigationRailItemDefaults.colors(indicatorColor = HalloweenIndicator)
    SeasonalTheme.NEW_YEAR, SeasonalTheme.NONE -> NavigationRailItemDefaults.colors()
}
