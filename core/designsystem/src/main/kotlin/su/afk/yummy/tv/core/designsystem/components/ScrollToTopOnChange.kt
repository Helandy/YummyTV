package su.afk.yummy.tv.core.designsystem.components

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.withFrameNanos
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

/**
 * Прокручивает список к началу, когда меняется порядок [order] (например, элемент со звездой
 * поднялся наверх). Первое значение пропускается: на старте положение не трогаем.
 */
@Composable
fun ScrollToTopOnChange(order: Any?, state: LazyListState) {
    var initial by remember { mutableStateOf(true) }
    LaunchedEffect(order) {
        if (initial) {
            initial = false
        } else {
            // Ждём кадр, пока список перестроится после смены порядка: иначе прокрутку перебивает
            // сдвиг к сфокусированному элементу (на ТВ фокус остаётся на звезде переехавшей строки).
            withFrameNanos { }
            state.animateScrollToItem(0)
            if (state.firstVisibleItemIndex != 0 || state.firstVisibleItemScrollOffset != 0) {
                state.scrollToItem(0)
            }
        }
    }
}

@Composable
fun ScrollToTopOnChange(order: Any?, state: ScrollState) {
    var initial by remember { mutableStateOf(true) }
    LaunchedEffect(order) {
        if (initial) initial = false else state.animateScrollTo(0)
    }
}
