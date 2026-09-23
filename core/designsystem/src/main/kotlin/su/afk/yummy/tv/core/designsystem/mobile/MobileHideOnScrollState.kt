package su.afk.yummy.tv.core.designsystem.mobile

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Состояние «шапка прячется при листании вниз и возвращается при листании вверх».
 *
 * Слушает вложенный скролл ([nestedScrollConnection]) и копит смещение: как только
 * подряд набралось больше [thresholdPx] в одну сторону — переключает [isVisible].
 * Ничего не потребляет, поэтому список под ним скроллится как обычно.
 */
@Stable
class MobileHideOnScrollState internal constructor(
    private val thresholdPx: Float,
) {
    /** Должна ли шапка сейчас показываться. */
    var isVisible by mutableStateOf(true)
        private set

    private var accumulated = 0f

    /** Принудительно раскрыть шапку (например, после смены фильтра). */
    fun show() {
        accumulated = 0f
        isVisible = true
    }

    private fun onScroll(deltaY: Float) {
        // Смена направления — копим заново.
        if ((deltaY < 0f) != (accumulated < 0f)) accumulated = 0f
        accumulated += deltaY
        when {
            accumulated <= -thresholdPx -> {
                isVisible = false
                accumulated = 0f
            }

            accumulated >= thresholdPx -> {
                isVisible = true
                accumulated = 0f
            }
        }
    }

    val nestedScrollConnection: NestedScrollConnection = object : NestedScrollConnection {
        override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
            onScroll(available.y)
            return Offset.Zero
        }
    }
}

/**
 * Создаёт [MobileHideOnScrollState].
 *
 * @param threshold насколько нужно пролистать в одну сторону, чтобы шапка сменила состояние.
 */
@Composable
fun rememberMobileHideOnScrollState(threshold: Dp = 12.dp): MobileHideOnScrollState {
    val thresholdPx = with(LocalDensity.current) { threshold.toPx() }
    return remember(thresholdPx) { MobileHideOnScrollState(thresholdPx) }
}
