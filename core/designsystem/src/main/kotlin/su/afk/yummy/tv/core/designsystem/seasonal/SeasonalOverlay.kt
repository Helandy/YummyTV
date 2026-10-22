package su.afk.yummy.tv.core.designsystem.seasonal

import su.afk.yummy.tv.core.model.seasonal.SeasonalTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/** Полноэкранный слой сезона: снегопад (Новый год) или плывущий призрак (Хэллоуин). Касания не перехватывает. */
@Composable
fun SeasonalOverlay(modifier: Modifier = Modifier) {
    val theme = rememberSeasonalTheme()
    if (theme == SeasonalTheme.NONE || !rememberSeasonalOverlayEnabled()) return
    when (theme) {
        SeasonalTheme.NEW_YEAR -> Snowfall(modifier)
        SeasonalTheme.HALLOWEEN -> GhostDrift(modifier)
        SeasonalTheme.NONE -> Unit
    }
}
