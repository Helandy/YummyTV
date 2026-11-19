package su.afk.yummy.tv.core.designsystem.mobile.tabs

import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.lazy.LazyListLayoutInfo
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.LayoutDirection
import kotlin.math.abs
import kotlin.math.pow

/** Степень опережения/догоняния краёв при перетекании между соседними плитками. */
private const val FLUID_STRETCH_EXPONENT = 1.6f

/** Возвращает `true`, если вкладки [fromIndex] и [toIndex] стоят рядом в одном ряду. */
fun areAdjacentRowTabs(fromIndex: Int, toIndex: Int): Boolean =
    fromIndex >= 0 && toIndex >= 0 && abs(fromIndex - toIndex) == 1

/**
 * Возвращает `true`, если плитки [fromIndex] и [toIndex] граничат по горизонтали или вертикали
 * в сетке с [columns] колонками (расстояние Манхэттена равно 1).
 *
 * Переход с конца одной строки в начало следующей (например, `3 -> 4` при `columns = 4`)
 * соседством не считается, чтобы индикатор не пересекал по диагонали чужие плитки.
 */
fun areAdjacentGridTabs(
    fromIndex: Int,
    toIndex: Int,
    columns: Int,
): Boolean {
    if (columns <= 0 || fromIndex < 0 || toIndex < 0 || fromIndex == toIndex) return false
    val rowDelta = abs(fromIndex / columns - toIndex / columns)
    val colDelta = abs(fromIndex % columns - toIndex % columns)
    return rowDelta + colDelta == 1
}

/**
 * Интерполирует отрезок индикатора между двумя соседними вкладками с плавным «перетеканием»:
 * передний край быстрее заходит в целевую плитку, а задний мягко подтягивается следом.
 *
 * Формула симметрична по времени и направлению (`0 -> 1` при `t` совпадает с `1 -> 0` при `1 - t`),
 * включая RTL-раскладку и разворот жеста на полпути.
 */
fun interpolateFluidSpan(
    fromStart: Float,
    fromEnd: Float,
    toStart: Float,
    toEnd: Float,
    fraction: Float,
): Pair<Float, Float> {
    val t = fraction.coerceIn(0f, 1f)
    if (t <= 0f) return minOf(fromStart, fromEnd) to maxOf(fromStart, fromEnd)
    if (t >= 1f) return minOf(toStart, toEnd) to maxOf(toStart, toEnd)

    val leading = 1f - (1f - t).pow(FLUID_STRETCH_EXPONENT)
    val trailing = t.pow(FLUID_STRETCH_EXPONENT)

    val start = if (toStart >= fromStart) {
        lerpFloat(fromStart, toStart, trailing)
    } else {
        lerpFloat(fromStart, toStart, leading)
    }
    val end = if (toEnd >= fromEnd) {
        lerpFloat(fromEnd, toEnd, leading)
    } else {
        lerpFloat(fromEnd, toEnd, trailing)
    }
    return minOf(start, end) to maxOf(start, end)
}

/**
 * Интерполирует прямоугольник индикатора между двумя соседними плитками:
 * растягивается по горизонтали для соседей по ряду и по вертикали для соседей по столбцу.
 */
fun interpolateFluidRect(
    from: Rect,
    to: Rect,
    fraction: Float,
): Rect {
    val (left, right) = interpolateFluidSpan(from.left, from.right, to.left, to.right, fraction)
    val (top, bottom) = interpolateFluidSpan(from.top, from.bottom, to.top, to.bottom, fraction)
    return Rect(left = left, top = top, right = right, bottom = bottom)
}

/**
 * Рисует скруглённую плашку активной вкладки: плавно перетекает между соседними плитками
 * и мягко меняет прозрачность на месте при переходе между несоседними.
 */
internal fun DrawScope.drawFluidPillIndicator(
    bounds: List<Rect?>,
    transition: MobileTabTransition,
    cornerRadius: CornerRadius,
    colorForIndex: (Int) -> Color,
    areAdjacent: (Int, Int) -> Boolean,
) {
    if (transition.isIdle) {
        val index = transition.activeIndex
        val rect = bounds.getOrNull(index) ?: return
        drawPillRect(rect, colorForIndex(index), cornerRadius)
        return
    }

    val fromRect = bounds.getOrNull(transition.fromIndex)
    val toRect = bounds.getOrNull(transition.toIndex)
    val fraction = transition.fraction.coerceIn(0f, 1f)
    val fromColor = colorForIndex(transition.fromIndex)
    val toColor = colorForIndex(transition.toIndex)

    if (fromRect != null && toRect != null && areAdjacent(transition.fromIndex, transition.toIndex)) {
        val blendedRect = interpolateFluidRect(fromRect, toRect, fraction)
        drawPillRect(blendedRect, lerp(fromColor, toColor, fraction), cornerRadius)
    } else {
        fromRect?.let { drawPillRect(it, fromColor.scaleAlpha(1f - fraction), cornerRadius) }
        toRect?.let { drawPillRect(it, toColor.scaleAlpha(fraction), cornerRadius) }
    }
}

/**
 * Рисует скруглённую плашку активной вкладки внутри горизонтального `LazyRow`:
 * между соседними элементами перетекает, между дальними — плавно затухает/проявляется.
 */
fun DrawScope.drawLazyRowPillIndicator(
    listState: LazyListState,
    transition: MobileTabTransition,
    color: Color,
    cornerRadius: CornerRadius,
    verticalInsetPx: Float = 0f,
) {
    val layoutInfo = listState.layoutInfo
    if (layoutInfo.visibleItemsInfo.isEmpty()) return
    val top = verticalInsetPx.coerceAtLeast(0f)
    val bottom = (size.height - top).coerceAtLeast(top)
    if (bottom <= top) return

    clipRect {
        if (transition.isIdle) {
            val span = layoutInfo.itemHorizontalSpan(transition.activeIndex, size.width, layoutDirection)
                ?: return@clipRect
            drawPillRect(Rect(span.first, top, span.second, bottom), color, cornerRadius)
            return@clipRect
        }

        val fromSpan = layoutInfo.itemHorizontalSpan(transition.fromIndex, size.width, layoutDirection)
        val toSpan = layoutInfo.itemHorizontalSpan(transition.toIndex, size.width, layoutDirection)
        val fraction = transition.fraction.coerceIn(0f, 1f)

        if (fromSpan != null && toSpan != null && areAdjacentRowTabs(transition.fromIndex, transition.toIndex)) {
            val (left, right) = interpolateFluidSpan(
                fromStart = fromSpan.first,
                fromEnd = fromSpan.second,
                toStart = toSpan.first,
                toEnd = toSpan.second,
                fraction = fraction,
            )
            drawPillRect(Rect(left, top, right, bottom), color, cornerRadius)
        } else {
            fromSpan?.let {
                drawPillRect(Rect(it.first, top, it.second, bottom), color.scaleAlpha(1f - fraction), cornerRadius)
            }
            toSpan?.let {
                drawPillRect(Rect(it.first, top, it.second, bottom), color.scaleAlpha(fraction), cornerRadius)
            }
        }
    }
}

/** Рисует нижнее подчёркивание активной вкладки для фиксированного `Row`. */
internal fun DrawScope.drawFluidUnderlineIndicator(
    bounds: List<Rect?>,
    transition: MobileTabTransition,
    color: Color,
    indicatorHeightPx: Float,
    topCornerRadiusPx: Float,
) {
    if (indicatorHeightPx <= 0f) return
    if (transition.isIdle) {
        val rect = bounds.getOrNull(transition.activeIndex) ?: return
        drawTopRoundedBar(rect.left, rect.right, color, indicatorHeightPx, topCornerRadiusPx)
        return
    }

    val fromRect = bounds.getOrNull(transition.fromIndex)
    val toRect = bounds.getOrNull(transition.toIndex)
    val fraction = transition.fraction.coerceIn(0f, 1f)

    if (fromRect != null && toRect != null && areAdjacentRowTabs(transition.fromIndex, transition.toIndex)) {
        val (left, right) = interpolateFluidSpan(fromRect.left, fromRect.right, toRect.left, toRect.right, fraction)
        drawTopRoundedBar(left, right, color, indicatorHeightPx, topCornerRadiusPx)
    } else {
        fromRect?.let {
            drawTopRoundedBar(it.left, it.right, color.scaleAlpha(1f - fraction), indicatorHeightPx, topCornerRadiusPx)
        }
        toRect?.let {
            drawTopRoundedBar(it.left, it.right, color.scaleAlpha(fraction), indicatorHeightPx, topCornerRadiusPx)
        }
    }
}

/** Рисует нижнее подчёркивание активной вкладки для прокручиваемого `LazyRow`. */
internal fun DrawScope.drawLazyRowUnderlineIndicator(
    listState: LazyListState,
    transition: MobileTabTransition,
    color: Color,
    indicatorHeightPx: Float,
    topCornerRadiusPx: Float,
) {
    val layoutInfo = listState.layoutInfo
    if (indicatorHeightPx <= 0f || layoutInfo.visibleItemsInfo.isEmpty()) return

    clipRect {
        if (transition.isIdle) {
            val span = layoutInfo.itemHorizontalSpan(transition.activeIndex, size.width, layoutDirection)
                ?: return@clipRect
            drawTopRoundedBar(span.first, span.second, color, indicatorHeightPx, topCornerRadiusPx)
            return@clipRect
        }

        val fromSpan = layoutInfo.itemHorizontalSpan(transition.fromIndex, size.width, layoutDirection)
        val toSpan = layoutInfo.itemHorizontalSpan(transition.toIndex, size.width, layoutDirection)
        val fraction = transition.fraction.coerceIn(0f, 1f)

        if (fromSpan != null && toSpan != null && areAdjacentRowTabs(transition.fromIndex, transition.toIndex)) {
            val (left, right) = interpolateFluidSpan(
                fromStart = fromSpan.first,
                fromEnd = fromSpan.second,
                toStart = toSpan.first,
                toEnd = toSpan.second,
                fraction = fraction,
            )
            drawTopRoundedBar(left, right, color, indicatorHeightPx, topCornerRadiusPx)
        } else {
            fromSpan?.let {
                drawTopRoundedBar(
                    left = it.first,
                    right = it.second,
                    color = color.scaleAlpha(1f - fraction),
                    heightPx = indicatorHeightPx,
                    topCornerRadiusPx = topCornerRadiusPx,
                )
            }
            toSpan?.let {
                drawTopRoundedBar(
                    left = it.first,
                    right = it.second,
                    color = color.scaleAlpha(fraction),
                    heightPx = indicatorHeightPx,
                    topCornerRadiusPx = topCornerRadiusPx,
                )
            }
        }
    }
}

/** Плавно подкручивает горизонтальный [LazyListState], удерживая вкладку [index] ближе к центру. */
suspend fun LazyListState.animateScrollTabIntoCenter(index: Int) {
    if (index < 0) return
    val info = layoutInfo
    if (info.totalItemsCount <= 0 || index >= info.totalItemsCount) return
    val target = info.visibleItemsInfo.firstOrNull { it.index == index }
    if (target != null) {
        val viewportCenter = (info.viewportStartOffset + info.viewportEndOffset) / 2
        val itemCenter = target.offset + target.size / 2
        val delta = (itemCenter - viewportCenter).toFloat()
        val canScrollInDirection = (delta < 0f && canScrollBackward) || (delta > 0f && canScrollForward)
        if (abs(delta) > 1f && canScrollInDirection) {
            animateScrollBy(delta)
        }
    } else {
        animateScrollToItem(index)
    }
}

private fun LazyListLayoutInfo.itemHorizontalSpan(
    index: Int,
    containerWidth: Float,
    layoutDirection: LayoutDirection,
): Pair<Float, Float>? {
    val item = visibleItemsInfo.firstOrNull { it.index == index } ?: return null
    val start = (item.offset + beforeContentPadding).toFloat()
    return if (layoutDirection == LayoutDirection.Ltr) {
        start to (start + item.size)
    } else {
        val right = containerWidth - start
        (right - item.size) to right
    }
}

private fun DrawScope.drawPillRect(
    rect: Rect,
    color: Color,
    cornerRadius: CornerRadius,
) {
    if (rect.width <= 0f || rect.height <= 0f || color.alpha <= 0f) return
    drawRoundRect(
        color = color,
        topLeft = Offset(rect.left, rect.top),
        size = Size(rect.width, rect.height),
        cornerRadius = cornerRadius,
    )
}

private fun DrawScope.drawTopRoundedBar(
    left: Float,
    right: Float,
    color: Color,
    heightPx: Float,
    topCornerRadiusPx: Float,
) {
    if (right <= left || heightPx <= 0f || color.alpha <= 0f) return
    val topRadius = CornerRadius(topCornerRadiusPx, topCornerRadiusPx)
    val path = Path().apply {
        addRoundRect(
            RoundRect(
                rect = Rect(
                    left = left,
                    top = (size.height - heightPx).coerceAtLeast(0f),
                    right = right,
                    bottom = size.height,
                ),
                topLeft = topRadius,
                topRight = topRadius,
                bottomLeft = CornerRadius.Zero,
                bottomRight = CornerRadius.Zero,
            ),
        )
    }
    drawPath(path = path, color = color)
}

private fun Color.scaleAlpha(factor: Float): Color =
    copy(alpha = (alpha * factor.coerceIn(0f, 1f)).coerceIn(0f, 1f))

internal fun lerpFloat(start: Float, stop: Float, fraction: Float): Float =
    start + (stop - start) * fraction
