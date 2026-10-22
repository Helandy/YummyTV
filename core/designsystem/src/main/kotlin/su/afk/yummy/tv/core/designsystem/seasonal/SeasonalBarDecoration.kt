package su.afk.yummy.tv.core.designsystem.seasonal

import su.afk.yummy.tv.core.model.seasonal.SeasonalTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/** Украшение верхней кромки бара для текущей праздничной темы; вне сезона ничего не рисует. */
@Composable
fun SeasonalBarDecoration(modifier: Modifier = Modifier) {
    when (rememberSeasonalTheme()) {
        SeasonalTheme.NEW_YEAR -> NewYearGarland(modifier)
        SeasonalTheme.HALLOWEEN -> HalloweenBarDecoration(modifier)
        SeasonalTheme.NONE -> Unit
    }
}
