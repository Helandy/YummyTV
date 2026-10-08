package su.afk.yummy.tv.core.designsystem.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import su.afk.yummy.tv.core.designsystem.theme.YummySemanticColors

fun Double.toRatingColor(): Color = when {
    this < 6.0 -> YummySemanticColors.RatingBadgeLow
    this < 8.0 -> YummySemanticColors.StatusPostponed
    else -> YummySemanticColors.RatingBadgeHigh
}

/**
 * Цвет рейтинга для текста прямо на фоне темы, без тёмной плашки: на светлом фоне базовые
 * светло-зелёный и янтарный сливаются с ним, поэтому там цвет затемняется.
 */
@Composable
fun Double.toRatingTextColor(): Color {
    val base = toRatingColor()
    return if (MaterialTheme.colorScheme.background.luminance() > LIGHT_BACKGROUND_LUMINANCE) {
        lerp(base, Color.Black, LIGHT_BACKGROUND_DARKEN_FRACTION)
    } else {
        base
    }
}

private const val LIGHT_BACKGROUND_LUMINANCE = 0.5f
private const val LIGHT_BACKGROUND_DARKEN_FRACTION = 0.45f
