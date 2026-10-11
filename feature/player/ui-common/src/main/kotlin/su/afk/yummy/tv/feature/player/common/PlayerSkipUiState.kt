package su.afk.yummy.tv.feature.player.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.snapshots.SnapshotStateList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/** Пропуск сегментов: подсветка кнопки, снекбар (3s) и уже пропущенные сегменты. */
@Stable
class PlayerSkipUiState internal constructor(
    private val scope: CoroutineScope,
    highlightedSkipKeyState: MutableState<String?>,
    snackbarTextState: MutableState<String?>,
    val dismissedSkipKeys: SnapshotStateList<String>,
    private val cancelledAutoSkipKeys: SnapshotStateList<String>,
) {
    var highlightedSkipKey: String? by highlightedSkipKeyState
    var snackbarText: String? by snackbarTextState
        private set

    private var snackbarJob: Job? = null

    /** Идущий отсчёт автопропуска; привязан к ключу сегмента, чтобы старый отсчёт не затёр новый. */
    private var autoSkipCountdown: AutoSkipCountdown? by mutableStateOf(null)

    /** Оставшиеся секунды автопропуска для сегмента [skipKey] или null, если отсчёта нет. */
    fun autoSkipRemainingSeconds(skipKey: String?): Int? =
        autoSkipCountdown?.takeIf { it.skipKey == skipKey }?.remainingSeconds

    /** Доля прошедшего отсчёта автопропуска (0..1) для сегмента [skipKey] или null. */
    fun autoSkipProgress(skipKey: String?): Float? =
        autoSkipCountdown?.takeIf { it.skipKey == skipKey }?.progress

    /** Отменяет автопропуск сегмента [skipKey]; ручная кнопка пропуска остаётся. */
    fun cancelAutoSkip(skipKey: String) {
        if (skipKey !in cancelledAutoSkipKeys) cancelledAutoSkipKeys.add(skipKey)
    }

    fun isAutoSkipCancelled(skipKey: String): Boolean = skipKey in cancelledAutoSkipKeys

    /**
     * Отсчитывает [seconds] перед автопропуском сегмента [skipKey]. Время идёт только
     * пока [isPlaying] — на паузе отсчёт замирает. Возвращает true, когда отсчёт дошёл до нуля,
     * и false, если автопропуск отменён [cancelAutoSkip]; отмена корутины (смена сегмента,
     * ручной пропуск) прерывает его.
     */
    suspend fun runAutoSkipCountdown(skipKey: String, seconds: Int, isPlaying: () -> Boolean): Boolean {
        val totalMs = seconds * 1000L
        var elapsedMs = 0L
        try {
            while (elapsedMs < totalMs) {
                if (isAutoSkipCancelled(skipKey)) return false
                autoSkipCountdown = AutoSkipCountdown(
                    skipKey = skipKey,
                    remainingSeconds = ((totalMs - elapsedMs + 999) / 1000).toInt(),
                    progress = elapsedMs.toFloat() / totalMs,
                )
                snapshotFlow { isPlaying() || isAutoSkipCancelled(skipKey) }.first { it }
                delay(AUTO_SKIP_TICK_MS.milliseconds)
                elapsedMs += AUTO_SKIP_TICK_MS
            }
            return !isAutoSkipCancelled(skipKey)
        } finally {
            if (autoSkipCountdown?.skipKey == skipKey) autoSkipCountdown = null
        }
    }

    fun showSnackbar(message: String) {
        snackbarText = message
        snackbarJob?.cancel()
        snackbarJob = scope.launch {
            delay(3.seconds)
            if (snackbarText == message) snackbarText = null
        }
    }

    fun cancel() {
        snackbarJob?.cancel()
    }

    private data class AutoSkipCountdown(
        val skipKey: String,
        val remainingSeconds: Int,
        val progress: Float,
    )

    companion object {
        /** Шаг отсчёта автопропуска; UI анимирует заполнение кнопки на этот же интервал. */
        const val AUTO_SKIP_TICK_MS = 100L
    }
}

/** [resetKey] — идентификатор серии: при её смене пропущенные сегменты и снекбар сбрасываются. */
@Composable
fun rememberPlayerSkipUiState(resetKey: Any): PlayerSkipUiState {
    val scope = rememberCoroutineScope()
    val highlightedSkipKey = remember { mutableStateOf<String?>(null) }
    val snackbarText = remember(resetKey) { mutableStateOf<String?>(null) }
    val dismissedSkipKeys = remember(resetKey) { mutableStateListOf<String>() }
    val cancelledAutoSkipKeys = remember(resetKey) { mutableStateListOf<String>() }
    return remember(snackbarText, dismissedSkipKeys, cancelledAutoSkipKeys) {
        PlayerSkipUiState(
            scope = scope,
            highlightedSkipKeyState = highlightedSkipKey,
            snackbarTextState = snackbarText,
            dismissedSkipKeys = dismissedSkipKeys,
            cancelledAutoSkipKeys = cancelledAutoSkipKeys,
        )
    }
}
