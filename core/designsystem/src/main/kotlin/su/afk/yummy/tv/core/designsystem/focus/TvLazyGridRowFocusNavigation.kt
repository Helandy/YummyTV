package su.afk.yummy.tv.core.designsystem.focus

import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Страховка для DPAD вверх/вниз в вертикальном гриде: переход на соседний ряд с сохранением колонки.
 *
 * Штатный focus search Compose, упираясь в ещё не скомпонованный ряд, подтягивает beyond-bounds
 * ровно один элемент (`addNextInterval` двигает границу на один индекс) и отдаёт фокус первому же
 * найденному — с 3-й карточки фокус прыгает на 1-ю (вверх — на последнюю).
 *
 * При [rememberTvTopAnchoredGridBringIntoViewSpec] ряд над сфокусированным всегда за верхней
 * кромкой, то есть не скомпонован — вверх страховка срабатывает на каждом шаге. Вниз — когда ряд
 * ещё не доехал до пивота, например при удержании DPAD.
 *
 * Если целевая карточка уже скомпонована, событие не перехватывается — работает обычный поиск
 * фокуса с анимированным bringIntoView.
 *
 * @param index индекс карточки в данных (не в lazy-списке).
 * @param columnCount число колонок грида — на столько сдвигается индекс за один ряд.
 * @param itemCount общее число карточек в данных.
 * @param focusRequesterAt requester карточки по её индексу в данных.
 * @param lazyIndexOffset сдвиг индекса данных до индекса в lazy-списке (шапка, спаны и т.п.).
 */
fun Modifier.tvLazyGridRowFocusNavigation(
    index: Int,
    columnCount: Int,
    itemCount: Int,
    gridState: LazyGridState,
    scope: CoroutineScope,
    focusRequesterAt: (Int) -> FocusRequester?,
    lazyIndexOffset: Int = 0,
): Modifier = onPreviewKeyEvent { event ->
    if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
    val targetIndex = when (event.key) {
        Key.DirectionDown -> index + columnCount
        Key.DirectionUp -> index - columnCount
        else -> return@onPreviewKeyEvent false
    }
    if (targetIndex !in 0 until itemCount) return@onPreviewKeyEvent false
    val targetLazyIndex = targetIndex + lazyIndexOffset
    if (gridState.isItemComposed(targetLazyIndex)) return@onPreviewKeyEvent false
    val targetFocusRequester = focusRequesterAt(targetIndex) ?: return@onPreviewKeyEvent false

    scope.launch {
        if (targetIndex < columnCount) {
            // Первый ряд — к самому началу, чтобы над ним снова показалась шапка грида.
            gridState.scrollToItem(0)
        } else {
            // Ряд встаёт туда же, куда его довёл бы спек: на межрядный отступ от кромки.
            gridState.scrollToItem(
                index = targetLazyIndex,
                scrollOffset = gridState.layoutInfo.beforeContentPadding -
                    gridState.layoutInfo.mainAxisItemSpacing,
            )
        }
        snapshotFlow { gridState.isItemComposed(targetLazyIndex) }.first { it }
        requestFocusUntilTimeout(targetFocusRequester)
    }
    true
}

private fun LazyGridState.isItemComposed(lazyIndex: Int): Boolean =
    layoutInfo.visibleItemsInfo.any { it.index == lazyIndex }
