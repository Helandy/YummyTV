package su.afk.yummy.tv.feature.details.full.model

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.layout.LayoutCoordinates

/**
 * Явный порядок фокуса рядов полной деталки по индексу ряда.
 *
 * Пространственный поиск «вверх» не находит предыдущий ряд, если тот не скомпонован в
 * [androidx.compose.foundation.lazy.LazyColumn], и фокус обходит список по кругу в конец.
 */
internal class FullDetailsFocusOrder(first: FocusRequester) {
    private val requesters = mutableMapOf(0 to first)

    fun requester(index: Int): FocusRequester = requesters.getOrPut(index) { FocusRequester() }
}

internal val LocalFullDetailsFocusOrder = staticCompositionLocalOf<FullDetailsFocusOrder?> { null }

/** Сообщает ряду-контейнеру координаты сфокусированного чипа. */
internal val LocalFocusedChipReporter = staticCompositionLocalOf<((LayoutCoordinates) -> Unit)?> { null }
