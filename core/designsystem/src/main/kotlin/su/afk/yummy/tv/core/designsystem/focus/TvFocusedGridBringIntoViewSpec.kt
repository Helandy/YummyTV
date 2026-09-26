package su.afk.yummy.tv.core.designsystem.focus

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.BringIntoViewSpec
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import kotlin.math.abs

/**
 * Стейтлес BringIntoViewSpec для TV-фокуса — чистая функция от (offset, size, containerSize),
 * без общего изменяемого состояния между вызовами (общий mutable object однажды уже ломал
 * скролл по всему приложению).
 *
 * [skipIfFullyVisible] — не скроллить, если элемент уже целиком помещается в контейнер; нужно,
 * чтобы переход фокуса поперёк оси скролла (например вбок внутри уже видимого ряда грида) не
 * давал лишний "подскролл".
 *
 * [centered] — если true, элемент центрируется: target = (containerSize - size) / 2, что
 * корректно учитывает размер самого элемента (в отличие от pivotFraction = 0.5f, который тянул
 * бы leading edge элемента к середине контейнера, а не центрировал бы сам элемент). Если false,
 * используется [pivotOffsetPx] (если задан) или [pivotFraction] от leading edge контейнера.
 *
 * [toleranceFraction] — доля размера элемента, в пределах которой промах мимо цели считается
 * нулевым. Нужна там, где выключен [skipIfFullyVisible]: сфокусированная карточка увеличена
 * `graphicsLayer`-скейлом ([tvFocusableClick]), а `ContentInViewNode` берёт границы узла уже с
 * трансформацией слоя — то есть её верх выше невыбранной на половину прироста (при скейле 1.04 это
 * 2% высоты). Без допуска каждый переход вбок доскролливал бы эту разницу, да ещё и по мере того,
 * как скейл анимируется пружиной — визуально это дёрганье экрана.
 */
@OptIn(ExperimentalFoundationApi::class)
class TvPivotBringIntoViewSpec(
    private val skipIfFullyVisible: Boolean = true,
    private val centered: Boolean = false,
    private val pivotFraction: Float = FocusedItemPivotFraction,
    private val toleranceFraction: Float = 0f,
    private val pivotOffsetPx: Float? = null,
) : BringIntoViewSpec {
    override fun calculateScrollDistance(
        offset: Float,
        size: Float,
        containerSize: Float,
    ): Float {
        if (containerSize <= 0f) return 0f

        if (skipIfFullyVisible) {
            val trailingEdge = offset + size
            if (offset >= 0f && trailingEdge <= containerSize) return 0f
        }

        val distance = when {
            size >= containerSize -> offset
            centered -> offset - (containerSize - size) / 2f
            pivotOffsetPx != null -> offset - pivotOffsetPx
            else -> offset - containerSize * pivotFraction
        }
        val tolerance = maxOf(MinScrollDistancePx, size * toleranceFraction)
        return if (abs(distance) <= tolerance) 0f else distance
    }
}

/** Прежнее поведение: пропускает уже видимые элементы, иначе тянет к 12% от leading edge. */
val TvFocusedGridBringIntoViewSpec: BringIntoViewSpec =
    TvPivotBringIntoViewSpec(skipIfFullyVisible = true, centered = false)

/**
 * Для вертикальных TV-гридов: сфокусированный ряд всегда встаёт к верхней кромке, а контент
 * прокручивается под ним (как у Netflix). Пивот — ровно [rowSpacing] от кромки: низ предыдущего
 * ряда оказывается на самой кромке и не виден, а место под скейл фокуса (1.04 ≈ 7dp) остаётся.
 * [rowSpacing] должен совпадать с `verticalArrangement` грида.
 *
 * `skipIfFullyVisible = false`: со skip спек пересчитывается каждый кадр анимации
 * (`ContentInViewNode.afterFrame`) и паркует ряд впритык к нижней кромке — фокус снова «ездит» по
 * экрану вместо контента. Лишнего подскролла при переходе вбок нет: ряд уже на пивоте, дистанция 0.
 *
 * Допуска на скейл фокуса здесь нет: карточки грида обязаны быть обёрнуты в
 * [tvWholeItemBringIntoView], который отдаёт неувеличенные границы ячейки (с допуском ряд
 * останавливался раньше пивота, и над ним торчал низ предыдущего ряда).
 *
 * Предыдущий ряд при таком пивоте не скомпонован — DPAD вверх/вниз страхует
 * [tvLazyGridRowFocusNavigation], шапку грида над первым рядом — [tvWholeItemBringIntoView].
 */
@Composable
fun rememberTvTopAnchoredGridBringIntoViewSpec(rowSpacing: Dp): BringIntoViewSpec {
    val pivotOffsetPx = with(LocalDensity.current) { rowSpacing.toPx() }
    return remember(pivotOffsetPx) {
        TvPivotBringIntoViewSpec(
            skipIfFullyVisible = false,
            pivotOffsetPx = pivotOffsetPx,
        )
    }
}

/**
 * Для вертикальных гридов, где промах должен центрировать ряд (а не подтягивать его к 12%
 * сверху), но переход фокуса вбок внутри уже видимого ряда по-прежнему не должен скроллить.
 */
val TvCenteredGridBringIntoViewSpec: BringIntoViewSpec =
    TvPivotBringIntoViewSpec(skipIfFullyVisible = true, centered = true)

/**
 * Для горизонтальных каруселей, где ось скролла совпадает с осью навигации — "skip if fully
 * visible" здесь не нужен (нет поперечного перехода, который надо защищать), а без него каждая
 * сфокусированная карточка стабильно центрируется, вместо чередования "не скроллим" / "тянем к
 * 12%", которое выглядит как дёрганье.
 */
val TvCenteredCarouselBringIntoViewSpec: BringIntoViewSpec =
    TvPivotBringIntoViewSpec(skipIfFullyVisible = false, centered = true)

internal const val FocusedItemPivotFraction = 0.12f

private const val MinScrollDistancePx = 1f
