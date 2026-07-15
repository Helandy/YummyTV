package su.afk.yummy.tv.feature.player.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration

/** Аккумулятор step-seek и тост с суммарным смещением. Сбрасывается по streamUrl. */
@Stable
class PlayerStepSeekToastState internal constructor(
    private val scope: CoroutineScope,
    private val toastDuration: Duration,
    val accumulator: StepSeekAccumulator = StepSeekAccumulator(),
) {
    var text: String? by mutableStateOf(null)
        private set
    var direction: StepSeekDirection by mutableStateOf(StepSeekDirection.Forward)
        private set
    private var toastJob: Job? = null

    fun nextOffsetMs(direction: StepSeekDirection, nowMs: Long): Long =
        accumulator.next(direction, nowMs)

    fun showToast(direction: StepSeekDirection) {
        text = accumulator.totalOffsetMs.formatSignedSeconds()
        this.direction = direction
        toastJob?.cancel()
        toastJob = scope.launch {
            delay(toastDuration)
            text = null
        }
    }

    fun clear() {
        toastJob?.cancel()
        text = null
    }

    fun cancel() {
        toastJob?.cancel()
    }
}

@Composable
fun rememberPlayerStepSeekToastState(
    streamUrl: String,
    toastDuration: Duration,
): PlayerStepSeekToastState {
    val scope = rememberCoroutineScope()
    return remember(streamUrl) {
        PlayerStepSeekToastState(scope = scope, toastDuration = toastDuration)
    }
}
