package su.afk.yummy.tv.feature.player.common

import android.os.SystemClock
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.MutableLongState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.media3.common.Player
import kotlinx.coroutines.delay

/** Варианты автовыключения в минутах; 0 — таймер отключён. */
object PlayerSleepTimerOptions {
    val minutes: List<Int> = listOf(10, 15, 20, 30, 45, 60)
}

/**
 * Таймер автовыключения: по истечении ставит [Player] на паузу. Живёт на время экрана плеера
 * (переживает поворот), в настройках не сохраняется.
 */
@Stable
class PlayerSleepTimerState internal constructor(
    private val endAtState: MutableLongState,
    private val selectedMinutesState: MutableIntState,
) {
    /** Выбранный пользователем интервал в минутах; 0 — таймер отключён. */
    val selectedMinutes: Int get() = selectedMinutesState.intValue

    /** Метка окончания в часах [SystemClock.elapsedRealtime]; 0 — таймера нет. */
    internal val endAtMs: Long get() = endAtState.longValue

    val isActive: Boolean get() = endAtMs > 0L

    /** Сколько минут осталось, округлено вверх; 0 — таймер отключён. */
    fun remainingMinutes(): Int {
        val end = endAtMs
        if (end <= 0L) return 0
        val left = (end - SystemClock.elapsedRealtime()).coerceAtLeast(0L)
        return ((left + MILLIS_IN_MINUTE - 1) / MILLIS_IN_MINUTE).toInt().coerceAtLeast(1)
    }

    /** [minutes] <= 0 отключает таймер. */
    fun set(minutes: Int) {
        selectedMinutesState.intValue = minutes.coerceAtLeast(0)
        endAtState.longValue =
            if (minutes > 0) SystemClock.elapsedRealtime() + minutes * MILLIS_IN_MINUTE else 0L
    }

    fun cancel() {
        selectedMinutesState.intValue = 0
        endAtState.longValue = 0L
    }

    private companion object {
        const val MILLIS_IN_MINUTE = 60_000L
    }
}

@Composable
fun rememberPlayerSleepTimer(
    player: Player?,
    onElapsed: () -> Unit = {},
): PlayerSleepTimerState {
    val endAt = rememberSaveable { mutableLongStateOf(0L) }
    val selected = rememberSaveable { mutableIntStateOf(0) }
    val state = remember(endAt, selected) { PlayerSleepTimerState(endAt, selected) }
    val currentOnElapsed by rememberUpdatedState(onElapsed)
    val endAtMs = state.endAtMs
    LaunchedEffect(player, endAtMs) {
        if (endAtMs <= 0L || player == null) return@LaunchedEffect
        delay((endAtMs - SystemClock.elapsedRealtime()).coerceAtLeast(0L))
        player.pause()
        state.cancel()
        currentOnElapsed()
    }
    return state
}
