package su.afk.yummy.tv.core.designsystem.components

import androidx.compose.ui.graphics.Color

/**
 * Цвет столбца оценки в распределении голосов, как на AniList: оттенок идёт от красно-оранжевого
 * (оценка 1) к зелёному (оценка 10), насыщенность и яркость одинаковы.
 */
fun scoreDistributionColor(score: Int): Color =
    Color.hsl(
        hue = HUE_PER_SCORE * score.coerceIn(MIN_SCORE, MAX_SCORE),
        saturation = SATURATION,
        lightness = LIGHTNESS,
    )

private const val MIN_SCORE = 1
private const val MAX_SCORE = 10
private const val HUE_PER_SCORE = 10f
private const val SATURATION = 0.65f
private const val LIGHTNESS = 0.5f
