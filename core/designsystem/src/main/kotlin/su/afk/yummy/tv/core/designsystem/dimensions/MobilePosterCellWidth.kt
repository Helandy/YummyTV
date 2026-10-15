package su.afk.yummy.tv.core.designsystem.dimensions

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.floor
import kotlin.math.max

/** Горизонтальный отступ списков/сеток постеров на мобилке. */
val MobilePosterGridHorizontalPadding: Dp = 16.dp

/**
 * Ширина ячейки, которую получает карточка в `GridCells.Adaptive(posterWidth)`: сетка растягивает
 * колонки на всю строку. Нужна рядам (LazyRow), чтобы их карточки совпадали с карточками в сетках.
 */
@Composable
fun currentMobilePosterCellWidth(availableWidth: Dp): Dp {
    val posterWidth = currentMobilePosterWidth()
    val spacing = currentMobilePosterSpacing()
    val contentWidth = availableWidth - MobilePosterGridHorizontalPadding * 2
    val columns = max(1, floor((contentWidth + spacing) / (posterWidth + spacing)).toInt())
    return (contentWidth - spacing * (columns - 1)) / columns
}
