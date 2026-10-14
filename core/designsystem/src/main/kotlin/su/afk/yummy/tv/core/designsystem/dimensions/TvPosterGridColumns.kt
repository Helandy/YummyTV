package su.afk.yummy.tv.core.designsystem.dimensions

import androidx.compose.ui.unit.Dp

/** Насколько карточка может сжаться, чтобы в ряд влезла ещё одна колонка. */
private const val MIN_CARD_SHRINK = 0.85f

/**
 * Число колонок ТВ-грида постеров.
 *
 * Берёт столько целых карточек [cardWidth], сколько помещается в [availableWidth], и добавляет ещё одну
 * колонку, если ради неё карточку достаточно сжать не сильнее чем до [MIN_CARD_SHRINK] от [cardWidth]:
 * вместо пустого остатка справа получается ещё один тайтл с теми же пропорциями.
 */
fun tvPosterGridColumns(availableWidth: Dp, cardWidth: Dp, spacing: Dp): Int {
    val fitting = ((availableWidth + spacing) / (cardWidth + spacing)).toInt().coerceAtLeast(1)
    val extra = fitting + 1
    val extraCardWidth = (availableWidth - spacing * (extra - 1)) / extra
    return if (extraCardWidth >= cardWidth * MIN_CARD_SHRINK) extra else fitting
}
