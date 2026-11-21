package su.afk.yummy.tv.core.designsystem.mobile.scroll

import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow

/**
 * Держит список в самом конце, если пользователь дочитал его до конца, при любом изменении
 * размера вьюпорта: нижний бар скрывается и возвращается вместе с экраном поверх, окно
 * пересоздаётся при смене конфигурации. LazyList при этом сохраняет смещение, а не расстояние до
 * конца, и после уменьшения вьюпорта нижние кнопки остаются под баром.
 *
 * «В конце» запоминается на жестах пользователя и переживает пересоздание. Как только он
 * прокрутил список вверх, привязка снимается.
 */
@Composable
fun LazyListState.PinToEndOnViewportChange() {
    var pinned by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(this) {
        var lastViewport = -1
        snapshotFlow {
            Triple(layoutInfo.viewportSize.height, canScrollForward, isScrollInProgress)
        }.collect { (viewport, canForward, scrolling) ->
            val viewportChanged = viewport != lastViewport
            lastViewport = viewport
            when {
                // Жест пользователя: привязка следует за тем, стоит ли он в конце.
                scrolling -> pinned = !canForward
                // Дошли до конца без жеста (например, список стал короче).
                !canForward -> pinned = true
                // Вьюпорт изменился, список был в конце, а теперь оторвался от него.
                viewportChanged && pinned -> {
                    // Шагами по высоте вьюпорта: огромное значение переполняет целое смещение
                    // LazyList, и список улетает в начало.
                    repeat(MAX_PIN_STEPS) {
                        if (canScrollForward) scrollBy(layoutInfo.viewportSize.height.toFloat())
                    }
                }
            }
        }
    }
}

/** Верхняя граница шагов прокрутки к концу: хватает на любой разумный размер ленты. */
private const val MAX_PIN_STEPS = 50
