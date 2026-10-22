package su.afk.yummy.tv.core.designsystem.components

import su.afk.yummy.tv.core.model.seasonal.SeasonalTheme
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.WavyProgressIndicatorDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import su.afk.yummy.tv.core.designsystem.seasonal.PumpkinLoader
import su.afk.yummy.tv.core.designsystem.seasonal.rememberSeasonalTheme

/**
 * Indeterminate circular loader used across the app; change the look of every spinner here.
 *
 * Renders the Material 3 Expressive wavy ring. Compact inline spinners (button, card badge) pass an
 * explicit [strokeWidth] and get the plain thin ring instead, because the wave collapses at small sizes.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun YummyLoadingIndicator(
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    strokeWidth: Dp = Dp.Unspecified,
) {
    if (strokeWidth == Dp.Unspecified && rememberSeasonalTheme() == SeasonalTheme.HALLOWEEN) {
        PumpkinLoader(modifier)
    } else if (strokeWidth == Dp.Unspecified) {
        CircularWavyProgressIndicator(
            modifier = modifier,
            color = color.takeOrElse { WavyProgressIndicatorDefaults.indicatorColor },
        )
    } else {
        CircularProgressIndicator(
            modifier = modifier,
            color = color.takeOrElse { ProgressIndicatorDefaults.circularColor },
            strokeWidth = strokeWidth,
        )
    }
}

private inline fun Color.takeOrElse(block: () -> Color): Color = if (this != Color.Unspecified) this else block()
