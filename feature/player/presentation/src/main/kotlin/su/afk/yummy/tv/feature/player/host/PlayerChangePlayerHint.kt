package su.afk.yummy.tv.feature.player.host

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import su.afk.yummy.tv.feature.player.PlayerState

/**
 * Таймер подсказки «сменить плеер».
 *
 * Один на экран: его запускают и загрузка потока, и восстановление Alloha, а отменяет любой
 * исход, после которого подсказка уже не нужна.
 */
internal class PlayerChangePlayerHint(
    private val scope: CoroutineScope,
    private val update: (PlayerState.State.() -> PlayerState.State) -> Unit,
) {
    private var job: Job? = null

    val isRunning: Boolean get() = job?.isActive == true

    /** Показывает подсказку через [delayMs], если к тому моменту [condition] ещё выполняется. */
    fun start(delayMs: Long, condition: () -> Boolean = { true }) {
        job?.cancel()
        job = scope.launch {
            delay(delayMs)
            if (condition()) update { copy(showChangePlayerHint = true) }
        }
    }

    fun cancel() {
        job?.cancel()
    }
}
