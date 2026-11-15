package su.afk.yummy.tv.core.designsystem.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
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
fun Double.toRatingTextColor(): Color = toRatingColor().onThemeBackground()
