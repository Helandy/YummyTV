package su.afk.yummy.tv.feature.details.details.view

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import su.afk.yummy.tv.core.designsystem.focus.requestFocusUntilTimeout

/**
 * Фокус кнопки в заголовке секции («Показать все», «Оценить»), которая недостижима стрелками вверх/вниз:
 * иначе фокус с соседней секции попадал бы на неё, а не на карточки. Кнопка открывается только стрелкой
 * в сторону заголовка с элемента, помеченного [opensOn], и снова закрывается, когда фокус с неё ушёл.
 */
@Stable
internal class HeaderActionFocus(private val scope: CoroutineScope) {
    val requester = FocusRequester()
    var reachable by mutableStateOf(false)
        private set

    fun onFocusChanged(focused: Boolean) {
        if (!focused) reachable = false
    }

    /** Нажатие любой из [keys] на элементе переводит фокус на кнопку заголовка. */
    fun opensOn(keys: List<Key>): Modifier = Modifier.onPreviewKeyEvent { event ->
        if (event.type == KeyEventType.KeyDown && event.key in keys) {
            reachable = true
            scope.launch { requestFocusUntilTimeout(requester) }
            true
        } else {
            false
        }
    }
}

@Composable
internal fun rememberHeaderActionFocus(): HeaderActionFocus {
    val scope = rememberCoroutineScope()
    return remember { HeaderActionFocus(scope) }
}

/**
 * Стрелки, уводящие на «Показать все» с карточки ряда: первая карточка — влево, последняя — вправо
 * (единственная — обе).
 */
internal fun edgeKeys(index: Int, lastIndex: Int): List<Key> = buildList {
    if (index == 0) add(Key.DirectionLeft)
    if (index == lastIndex) add(Key.DirectionRight)
}
