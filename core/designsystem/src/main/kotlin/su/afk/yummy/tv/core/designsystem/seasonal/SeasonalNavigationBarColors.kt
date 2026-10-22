package su.afk.yummy.tv.core.designsystem.seasonal

import su.afk.yummy.tv.core.model.seasonal.SeasonalTheme
import androidx.compose.material3.NavigationBarItemColors
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val HalloweenIndicator = Color(0x66FF7A1A)

/** Цвета пунктов нижнего бара: на Хэллоуин индикатор активной вкладки становится тыквенным. */
@Composable
fun seasonalNavigationBarItemColors(): NavigationBarItemColors = when (rememberSeasonalTheme()) {
    SeasonalTheme.HALLOWEEN -> NavigationBarItemDefaults.colors(indicatorColor = HalloweenIndicator)
    SeasonalTheme.NEW_YEAR, SeasonalTheme.NONE -> NavigationBarItemDefaults.colors()
}
