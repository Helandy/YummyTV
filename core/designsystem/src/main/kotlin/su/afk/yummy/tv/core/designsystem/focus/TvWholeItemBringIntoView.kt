package su.afk.yummy.tv.core.designsystem.focus

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.relocation.BringIntoViewResponder
import androidx.compose.foundation.relocation.bringIntoViewResponder
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize

/**
 * Ячейка грида на [rememberTvTopAnchoredGridBringIntoViewSpec]: любой запрос bringIntoView изнутри
 * (фокус на карточке или на кнопках под ней) подменяется прямоугольником всей ячейки в её обычном,
 * неувеличенном размере. Вешается на внешний модификатор карточки — снаружи скейла фокуса.
 *
 * Зачем:
 * - Скейл фокуса ([tvFocusableClick], 1.04) — `graphicsLayer`, и `ContentInViewNode` видит уже
 *   увеличенные границы: верх выше реального на ~7dp, да ещё меняется по ходу анимации пружины.
 *   Ряд вставал не на пивот, а над кромкой торчал низ предыдущего ряда. Здесь размер берётся из
 *   layout, скейл на него не влияет.
 * - Кнопки под карточкой («Детали», «Удалить»): без подмены на пивот встаёт сама кнопка, а
 *   карточка уезжает за кромку. Параллельный запрос на ячейку не помогает — `ContentInViewNode`
 *   снимает запрос кнопки, только когда она сама дошла до пивота, и доскролливает.
 *
 * [gridStart] — только для первого ряда: прямоугольник расширяется вверх до начала контента, и
 * вместе с рядом видна шапка грида («Коллекции», «N серий» и т.п.) и её фокусируемые элементы.
 */
@OptIn(ExperimentalFoundationApi::class)
fun Modifier.tvWholeItemBringIntoView(gridStart: TvGridStartExtent? = null): Modifier = composed {
    val responder = remember { WholeItemBringIntoViewResponder() }
    responder.gridStart = gridStart
    onSizeChanged { responder.size = it }.bringIntoViewResponder(responder)
}

@OptIn(ExperimentalFoundationApi::class)
private class WholeItemBringIntoViewResponder : BringIntoViewResponder {
    var size: IntSize = IntSize.Zero
    var gridStart: TvGridStartExtent? = null

    override fun calculateRectForParent(localRect: Rect): Rect {
        val itemRect = if (size == IntSize.Zero) {
            localRect
        } else {
            Rect(0f, 0f, size.width.toFloat(), size.height.toFloat())
        }
        val extent = gridStart?.px(itemRect.height) ?: return itemRect
        return itemRect.copy(top = itemRect.top - extent)
    }

    // Сам элемент ничего не скроллит — скроллит родительский грид по прямоугольнику выше.
    override suspend fun bringChildIntoView(localRect: () -> Rect?) = Unit
}

/**
 * Шапка грида над первым рядом: фокус на её элементах всегда возвращает грид к началу, чтобы шапка
 * была видна целиком (а не уезжала на пивот верхнего якоря). Подходит только шапке, которая
 * помещается в экран.
 */
@OptIn(ExperimentalFoundationApi::class)
fun Modifier.tvGridHeaderBringIntoView(): Modifier = composed {
    val responder = remember { GridHeaderBringIntoViewResponder() }
    onSizeChanged { responder.size = it }.bringIntoViewResponder(responder)
}

@OptIn(ExperimentalFoundationApi::class)
private class GridHeaderBringIntoViewResponder : BringIntoViewResponder {
    var size: IntSize = IntSize.Zero

    override fun calculateRectForParent(localRect: Rect): Rect {
        if (size == IntSize.Zero) return localRect
        // Верх прямоугольника заведомо выше начала контента: грид прокручивается до упора вверх.
        return Rect(0f, -HEADER_REVEAL_OVERSHOOT_PX, size.width.toFloat(), size.height.toFloat())
    }

    override suspend fun bringChildIntoView(localRect: () -> Rect?) = Unit
}

private const val HEADER_REVEAL_OVERSHOOT_PX = 100_000f
