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

/** Горизонтальный отрезок элемента внутри контейнера `LazyRow` в координатах холста. */
internal data class HorizontalSpan(
    val start: Float,
    val end: Float,
) {
    val width: Float get() = (end - start).coerceAtLeast(0f)

    /** Доля ширины элемента, видимая в окне `[viewportStart, viewportEnd]` (`0f..1f`). */
    fun visibilityFraction(
        viewportStart: Float,
        viewportEnd: Float,
    ): Float {
        val w = width
        if (w <= 0f || viewportEnd <= viewportStart) return 0f
        val visibleStart = maxOf(start, viewportStart)
        val visibleEnd = minOf(end, viewportEnd)
        return ((visibleEnd - visibleStart).coerceAtLeast(0f) / w).coerceIn(0f, 1f)
    }
}

/** Возвращает `true`, если вкладки [fromIndex] и [toIndex] стоят рядом в одном ряду. */
internal fun areAdjacentRowTabs(fromIndex: Int, toIndex: Int): Boolean =
    fromIndex >= 0 && toIndex >= 0 && abs(fromIndex - toIndex) == 1

/**
 * Возвращает `true`, если плитки [fromIndex] и [toIndex] граничат по горизонтали или вертикали
 * в сетке с [columns] колонками (расстояние Манхэттена равно 1).
 *
 * Переход с конца одной строки в начало следующей (например, `3 -> 4` при `columns = 4`)
 * соседством не считается, чтобы индикатор не пересекал по диагонали чужие плитки.
 */
internal fun areAdjacentGridTabs(
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
internal fun interpolateFluidSpan(
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
internal fun interpolateFluidRect(
    from: Rect,
    to: Rect,
    fraction: Float,
): Rect {
    val (left, right) = interpolateFluidSpan(from.left, from.right, to.left, to.right, fraction)
    val (top, bottom) = interpolateFluidSpan(from.top, from.bottom, to.top, to.bottom, fraction)
    return Rect(left = left, top = top, right = right, bottom = bottom)
}

/**
 * Оценивает положение соседней вкладки [missingIndex], которая пока находится за краем
 * `LazyRow`, опираясь на видимого соседа [anchor] ([anchorIndex]) и шаг [spacingPx].
 */
internal fun estimateAdjacentOffscreenSpan(
    anchor: HorizontalSpan,
    anchorIndex: Int,
    missingIndex: Int,
    spacingPx: Float,
    layoutDirection: LayoutDirection,
): HorizontalSpan {
    val gap = spacingPx.coerceAtLeast(0f)
    val width = anchor.width
    val isToTheRight = (missingIndex > anchorIndex) == (layoutDirection == LayoutDirection.Ltr)
    return if (isToTheRight) {
        val start = anchor.end + gap
        HorizontalSpan(start = start, end = start + width)
    } else {
        val end = anchor.start - gap
        HorizontalSpan(start = end - width, end = end)
    }
}

/**
 * Единая логика отрисовки индикатора вкладки (фиксированный `Row`, сетка и `LazyRow`):
 * - в покое рисует активный элемент с полной непрозрачностью;
 * - между соседними вкладками ([areAdjacent]) перетекает через [drawBlended], при частичной
 *   видимости в `LazyRow` достраивая соседа за краем через [fallbackAdjacent];
 * - между дальними вкладками выполняет кроссфейд `1 - fraction` / `fraction`, учитывая
 *   частичную видимость каждого края ([crossfadeVisibility]).
 */
internal inline fun <T : Any> renderTabIndicator(
    transition: MobileTabTransition,
    itemAt: (index: Int) -> T?,
    areAdjacent: (from: Int, to: Int) -> Boolean,
    fallbackAdjacent: (anchor: T, anchorIndex: Int, missingIndex: Int) -> T? = { _, _, _ -> null },
    crossfadeVisibility: (item: T) -> Float = { 1f },
    drawSingle: (item: T, index: Int, alphaFactor: Float) -> Unit,
    drawBlended: (from: T, to: T, fromIndex: Int, toIndex: Int, fraction: Float) -> Unit,
) {
    if (transition.isIdle) {
        val index = transition.activeIndex
        val item = itemAt(index) ?: return
        drawSingle(item, index, 1f)
        return
    }

    val fromIndex = transition.fromIndex
    val toIndex = transition.toIndex
    val fraction = transition.fraction.coerceIn(0f, 1f)
    val adjacent = areAdjacent(fromIndex, toIndex)
    val rawFrom = itemAt(fromIndex)
    val rawTo = itemAt(toIndex)

    if (adjacent) {
        val resolvedFrom = rawFrom ?: rawTo?.let { fallbackAdjacent(it, toIndex, fromIndex) }
        val resolvedTo = rawTo ?: rawFrom?.let { fallbackAdjacent(it, fromIndex, toIndex) }
        if (resolvedFrom != null && resolvedTo != null) {
            drawBlended(resolvedFrom, resolvedTo, fromIndex, toIndex, fraction)
            return
        }
    }

    rawFrom?.let { item ->
        val alpha = (1f - fraction) * crossfadeVisibility(item).coerceIn(0f, 1f)
        if (alpha > 0f) drawSingle(item, fromIndex, alpha)
    }
    rawTo?.let { item ->
        val alpha = fraction * crossfadeVisibility(item).coerceIn(0f, 1f)
        if (alpha > 0f) drawSingle(item, toIndex, alpha)
    }
}

/** Рисует скруглённую плашку активной вкладки для фиксированного `Row` или сетки. */
internal fun DrawScope.drawFluidPillIndicator(
    boundsAt: (Int) -> Rect?,
    transition: MobileTabTransition,
    cornerRadius: CornerRadius,
    colorForIndex: (Int) -> Color,
    areAdjacent: (Int, Int) -> Boolean,
) {
    renderTabIndicator(
        transition = transition,
        itemAt = boundsAt,
        areAdjacent = areAdjacent,
        drawSingle = { rect, index, alpha ->
            drawPillRect(rect, colorForIndex(index).scaleAlpha(alpha), cornerRadius)
        },
        drawBlended = { from, to, fromIndex, toIndex, fraction ->
            drawPillRect(
                rect = interpolateFluidRect(from, to, fraction),
                color = lerp(colorForIndex(fromIndex), colorForIndex(toIndex), fraction),
                cornerRadius = cornerRadius,
            )
        },
    )
}

/** Рисует нижнее подчёркивание активной вкладки для фиксированного `Row`. */
internal fun DrawScope.drawFluidUnderlineIndicator(
    bounds: List<Rect?>,
    transition: MobileTabTransition,
    color: Color,
    indicatorHeightPx: Float,
    topCornerRadiusPx: Float,
    scratchPath: Path,
) {
    if (indicatorHeightPx <= 0f) return
    renderTabIndicator(
        transition = transition,
        itemAt = bounds::getOrNull,
        areAdjacent = ::areAdjacentRowTabs,
        drawSingle = { rect, _, alpha ->
            drawTopRoundedBar(
                left = rect.left,
                right = rect.right,
                color = color.scaleAlpha(alpha),
                heightPx = indicatorHeightPx,
                topCornerRadiusPx = topCornerRadiusPx,
                path = scratchPath,
            )
        },
        drawBlended = { from, to, _, _, fraction ->
            val (left, right) = interpolateFluidSpan(from.left, from.right, to.left, to.right, fraction)
            drawTopRoundedBar(left, right, color, indicatorHeightPx, topCornerRadiusPx, scratchPath)
        },
    )
}

/** Рисует скруглённую плашку активной вкладки внутри прокручиваемого `LazyRow`. */
internal fun DrawScope.drawLazyRowPillIndicator(
    listState: LazyListState,
    transition: MobileTabTransition,
    color: Color,
    cornerRadius: CornerRadius,
    verticalInsetPx: Float = 0f,
) {
    val top = verticalInsetPx.coerceAtLeast(0f)
    val bottom = (size.height - top).coerceAtLeast(top)
    if (bottom <= top) return
    drawLazyRowSpanIndicator(
        listState = listState,
        transition = transition,
    ) { left, right, alpha ->
        drawPillRect(Rect(left, top, right, bottom), color.scaleAlpha(alpha), cornerRadius)
    }
}

/** Рисует нижнее подчёркивание активной вкладки для прокручиваемого `LazyRow`. */
internal fun DrawScope.drawLazyRowUnderlineIndicator(
    listState: LazyListState,
    transition: MobileTabTransition,
    color: Color,
    indicatorHeightPx: Float,
    topCornerRadiusPx: Float,
    scratchPath: Path,
) {
    if (indicatorHeightPx <= 0f) return
    drawLazyRowSpanIndicator(
        listState = listState,
        transition = transition,
    ) { left, right, alpha ->
        drawTopRoundedBar(
            left = left,
            right = right,
            color = color.scaleAlpha(alpha),
            heightPx = indicatorHeightPx,
            topCornerRadiusPx = topCornerRadiusPx,
            path = scratchPath,
        )
    }
}

private inline fun DrawScope.drawLazyRowSpanIndicator(
    listState: LazyListState,
    transition: MobileTabTransition,
    drawSpan: (left: Float, right: Float, alphaFactor: Float) -> Unit,
) {
    val layoutInfo = listState.layoutInfo
    if (layoutInfo.visibleItemsInfo.isEmpty() || size.width <= 0f) return
    val spacingPx = layoutInfo.mainAxisItemSpacing.toFloat()

    clipRect {
        renderTabIndicator(
            transition = transition,
            itemAt = { index -> layoutInfo.itemHorizontalSpan(index, size.width, layoutDirection) },
            areAdjacent = ::areAdjacentRowTabs,
            fallbackAdjacent = { anchor, anchorIndex, missingIndex ->
                estimateAdjacentOffscreenSpan(anchor, anchorIndex, missingIndex, spacingPx, layoutDirection)
            },
            crossfadeVisibility = { span -> span.visibilityFraction(0f, size.width) },
            drawSingle = { span, _, alpha -> drawSpan(span.start, span.end, alpha) },
            drawBlended = { from, to, _, _, fraction ->
                val (left, right) = interpolateFluidSpan(from.start, from.end, to.start, to.end, fraction)
                drawSpan(left, right, 1f)
            },
        )
    }
}

/** Индекс вкладки, к которой следует подкручивать `LazyRow` (для дальних прыжков — сразу [MobileTabTransition.toIndex]). */
internal fun MobileTabTransition.lazyRowScrollTarget(): Int =
    if (!isIdle && !areAdjacentRowTabs(fromIndex, toIndex)) toIndex else activeIndex

/** Плавно подкручивает горизонтальный [LazyListState], удерживая вкладку [index] ближе к центру. */
internal suspend fun LazyListState.animateScrollTabIntoCenter(index: Int) {
    if (index < 0) return
    val info = layoutInfo
    if (info.totalItemsCount <= 0 || index >= info.totalItemsCount) return
    val viewportWidth = info.viewportEndOffset - info.viewportStartOffset
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
        val visible = info.visibleItemsInfo
        val estimatedSize = if (visible.isNotEmpty()) {
            visible.sumOf { it.size } / visible.size
        } else {
            0
        }
        val centerOffset = if (viewportWidth > estimatedSize && estimatedSize > 0) {
            -((viewportWidth - estimatedSize) / 2)
        } else {
            0
        }
        animateScrollToItem(index = index, scrollOffset = centerOffset)
    }
}

private fun LazyListLayoutInfo.itemHorizontalSpan(
    index: Int,
    containerWidth: Float,
    layoutDirection: LayoutDirection,
): HorizontalSpan? {
    val item = visibleItemsInfo.firstOrNull { it.index == index } ?: return null
    val start = (item.offset + beforeContentPadding).toFloat()
    return if (layoutDirection == LayoutDirection.Ltr) {
        HorizontalSpan(start = start, end = start + item.size)
    } else {
        val right = containerWidth - start
        HorizontalSpan(start = right - item.size, end = right)
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
    path: Path,
) {
    if (right <= left || heightPx <= 0f || color.alpha <= 0f) return
    val topRadius = CornerRadius(topCornerRadiusPx, topCornerRadiusPx)
    path.reset()
    path.addRoundRect(
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
    drawPath(path = path, color = color)
}

private fun Color.scaleAlpha(factor: Float): Color =
    copy(alpha = (alpha * factor.coerceIn(0f, 1f)).coerceIn(0f, 1f))

internal fun lerpFloat(start: Float, stop: Float, fraction: Float): Float =
    start + (stop - start) * fraction
