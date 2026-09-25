package su.afk.yummy.tv.core.designsystem.components

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
import kotlin.time.Duration.Companion.seconds

/**
 * Показ [GlobalToastOverlay]: сообщение приходит одноразовым эффектом ViewModel и живёт только
 * в UI, поэтому после пересоздания экрана тост не всплывает повторно, как было бы из state.
 */
@Stable
class GlobalToastState internal constructor(private val scope: CoroutineScope) {
    var message by mutableStateOf<String?>(null)
        private set

    private var hideJob: Job? = null

    /** Показать [message]; новое сообщение заменяет текущее и заново запускает таймер скрытия. */
    fun show(message: String) {
        hideJob?.cancel()
        this.message = message
        hideJob = scope.launch {
            delay(GLOBAL_TOAST_DURATION)
            this@GlobalToastState.message = null
        }
    }
}

/** Таймер скрытия отменяется вместе с [rememberCoroutineScope], когда экран уходит из композиции. */
@Composable
fun rememberGlobalToastState(): GlobalToastState {
    val scope = rememberCoroutineScope()
    return remember(scope) { GlobalToastState(scope) }
}

private val GLOBAL_TOAST_DURATION = 3.seconds
