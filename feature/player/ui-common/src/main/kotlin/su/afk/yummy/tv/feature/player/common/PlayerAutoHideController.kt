package su.afk.yummy.tv.feature.player.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration

/** Авто-скрытие оверлея: отменяет предыдущий таймер, прячет по guard-условию. */
@Stable
class PlayerAutoHideController internal constructor(
    private val scope: CoroutineScope,
    private val hideDelay: () -> Duration,
    private val canHide: () -> Boolean,
    private val onHide: () -> Unit,
) {
    private var hideJob: Job? = null

    fun schedule() {
        hideJob?.cancel()
        hideJob = scope.launch {
            delay(hideDelay())
            if (canHide()) onHide()
        }
    }

    fun cancel() {
        hideJob?.cancel()
    }
}

@Composable
fun rememberPlayerAutoHideController(
    hideDelay: Duration,
    canHide: () -> Boolean,
    onHide: () -> Unit,
): PlayerAutoHideController {
    val scope = rememberCoroutineScope()
    val currentHideDelay = rememberUpdatedState(hideDelay)
    val currentCanHide = rememberUpdatedState(canHide)
    val currentOnHide = rememberUpdatedState(onHide)
    val controller = remember {
        PlayerAutoHideController(
            scope = scope,
            hideDelay = { currentHideDelay.value },
            canHide = { currentCanHide.value.invoke() },
            onHide = { currentOnHide.value.invoke() },
        )
    }
    DisposableEffect(controller) {
        onDispose { controller.cancel() }
    }
    return controller
}
