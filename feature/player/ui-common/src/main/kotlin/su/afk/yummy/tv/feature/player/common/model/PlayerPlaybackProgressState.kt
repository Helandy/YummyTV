package su.afk.yummy.tv.feature.player.common.model

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

/**
 * Позиция/длительность/буфер и seek-состояние плеера (ТВ и мобилка), обновляемые polling-циклом
 * и перемоткой. Читается там, где рисуется таймлайн, чтобы тик позиции не перекомпоновывал
 * весь экран плеера.
 */
@Stable
class PlayerPlaybackProgressState(
    initialPositionMs: Long = 0L,
    initialDurationMs: Long = 0L,
) {
    var currentPosition: Long by mutableLongStateOf(initialPositionMs)
    var duration: Long by mutableLongStateOf(initialDurationMs)
    var bufferedProgress: Float by mutableFloatStateOf(0f)
    var isSeeking: Boolean by mutableStateOf(false)
    var seekProgress: Float by mutableFloatStateOf(0f)
    var lastSeekTimeMs: Long by mutableLongStateOf(0L)
    var lastPositionNotifyTimeMs: Long = 0L

    val displayTimeMs: Long
        get() = if (isSeeking && duration > 0) (seekProgress * duration).toLong() else currentPosition

    val progressFraction: Float
        get() = when {
            isSeeking -> seekProgress
            duration > 0 -> currentPosition.toFloat() / duration
            else -> 0f
        }
}

/** Держатель пересоздаётся при смене [keys] и начинает со стартовых позиции/длительности. */
@Composable
fun rememberPlayerPlaybackProgressState(
    vararg keys: Any?,
    initialPositionMs: Long = 0L,
    initialDurationMs: Long = 0L,
): PlayerPlaybackProgressState = remember(*keys) {
    PlayerPlaybackProgressState(initialPositionMs, initialDurationMs)
}
