package su.afk.yummy.tv.feature.details.full.view

import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import su.afk.yummy.tv.feature.details.full.model.LocalFocusedChipReporter
import su.afk.yummy.tv.feature.details.full.model.LocalFullDetailsFocusOrder

@Composable
internal fun FocusableDetailsItem(
    index: Int,
    listState: LazyListState,
    firstFocusRequester: FocusRequester? = null,
    focusable: Boolean = true,
    onPreviewKeyEvent: ((KeyEvent) -> Boolean)? = null,
    content: @Composable () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val previousRowPeekPx = with(density) { PreviousRowPeek.roundToPx() }
    val firstLineBottomPx = with(density) { FirstChipLineBottom.toPx() }
    val focusOrder = LocalFullDetailsFocusOrder.current
    var rowCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    var focusedChip by remember { mutableStateOf<LayoutCoordinates?>(null) }
    var isFocused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(8.dp)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                when {
                    firstFocusRequester != null -> Modifier.focusRequester(firstFocusRequester)
                    focusOrder != null -> Modifier.focusRequester(focusOrder.requester(index))
                    else -> Modifier
                },
            )
            .onGloballyPositioned { rowCoordinates = it }
            .onFocusChanged { focusState ->
                // Терминальный ряд фокусируется сам (isFocused); ряд-контейнер с
                // фокусируемыми чипами — по hasFocus (фокус у дочернего чипа).
                val focused = if (focusable) focusState.isFocused else focusState.hasFocus
                isFocused = focused
                if (focused) {
                    scope.launch {
                        // Предыдущий ряд оставляем слегка видимым: он должен быть скомпонован,
                        // чтобы фокус вверх мог на него перейти.
                        listState.scrollToItem(index, scrollOffset = -previousRowPeekPx)
                    }
                }
            }
            .then(
                if (onPreviewKeyEvent != null) {
                    Modifier.onPreviewKeyEvent(onPreviewKeyEvent)
                } else {
                    Modifier
                },
            )
            .onPreviewKeyEvent { event ->
                // Пространственный поиск «вверх» не находит нескомпонованный предыдущий ряд и
                // обходит список по кругу в конец, поэтому переходим на него явно. Внутри ряда
                // чипов (не первая строка) оставляем штатное движение фокуса.
                val leavesRow = focusable || isOnFirstChipLine(rowCoordinates, focusedChip, firstLineBottomPx)
                val previousVisible = index > 0 &&
                    listState.layoutInfo.visibleItemsInfo.any { it.index == index - 1 }
                if (event.type == KeyEventType.KeyDown && event.key == Key.DirectionUp &&
                    focusOrder != null && leavesRow && previousVisible
                ) {
                    focusOrder.requester(index - 1).requestFocus()
                } else {
                    false
                }
            }
            .then(if (focusable) Modifier.focusable() else Modifier.focusGroup())
            .background(
                // Подсвечиваем только терминальные текстовые ряды; в рядах с чипами
                // подсветку рисуют сами чипы (tvFocusableClick).
                color = if (isFocused && focusable) {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f)
                } else {
                    MaterialTheme.colorScheme.surface
                },
                shape = shape,
            )
            .padding(horizontal = 14.dp, vertical = 9.dp),
    ) {
        CompositionLocalProvider(LocalFocusedChipReporter provides { focusedChip = it }) {
            content()
        }
    }
}

private val PreviousRowPeek = 12.dp

/** Нижняя граница первой строки чипов от верха ряда: паддинг 9dp + высота чипа ~36dp + запас. */
private val FirstChipLineBottom = 60.dp

/** Сфокусированный чип лежит на первой строке ряда (положение читается в момент нажатия). */
private fun isOnFirstChipLine(
    row: LayoutCoordinates?,
    chip: LayoutCoordinates?,
    firstLineBottomPx: Float,
): Boolean {
    if (row == null || chip == null || !row.isAttached || !chip.isAttached) return true
    return chip.positionInRoot().y - row.positionInRoot().y < firstLineBottomPx
}
