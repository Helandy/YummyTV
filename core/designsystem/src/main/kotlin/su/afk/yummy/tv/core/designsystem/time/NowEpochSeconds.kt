package su.afk.yummy.tv.core.designsystem.time

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import kotlinx.coroutines.delay
import su.afk.yummy.tv.core.designsystem.locals.LocalNowMillis

/**
 * Текущее время в epoch-секундах, обновляется раз в [tickMillis]. Нужен экранам, где живёт
 * обратный отсчёт (например, до выхода серии). Держать один вызов на экран и передавать значение
 * вниз — корутина на каждую карточку списка обходится дорого.
 */
@Composable
fun rememberNowEpochSeconds(tickMillis: Long = DEFAULT_TICK_MILLIS): Long {
    val nowMillis = LocalNowMillis.current
    val nowEpochSeconds by produceState(
        initialValue = nowMillis() / 1_000L,
        key1 = tickMillis,
    ) {
        while (true) {
            value = nowMillis() / 1_000L
            delay(tickMillis)
        }
    }
    return nowEpochSeconds
}

private const val DEFAULT_TICK_MILLIS = 60_000L
