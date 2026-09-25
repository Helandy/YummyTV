package su.afk.yummy.tv.core.designsystem.components.lazy

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

/**
 * Держит ряд в начале, когда в голову списка добавились новые элементы.
 *
 * Lazy-список якорится по ключу первого видимого элемента, поэтому без этого после обновления
 * данных (кэш → свежий ответ) ряд остаётся на старой первой карточке, а новые оказываются
 * слева за краем. Проверка идёт в композиции, до measure: `firstVisibleItemIndex` ещё
 * отражает старую раскладку, и по нему видно, стоял ли пользователь в начале. Если ряд
 * проскроллен вглубь, позиция не трогается.
 */
@Composable
fun KeepLazyListAtStartOnNewHead(
    state: LazyListState,
    headKey: Any?,
    enabled: Boolean = true,
) {
    val previous = remember { PreviousHeadKey(headKey) }
    if (previous.value == headKey) return
    previous.value = headKey
    if (enabled && state.firstVisibleItemIndex == 0) {
        state.requestScrollToItem(0)
    }
}

private class PreviousHeadKey(var value: Any?)
