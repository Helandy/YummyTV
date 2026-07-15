package su.afk.yummy.tv.feature.player.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import su.afk.yummy.tv.feature.player.PlayerState

/**
 * Отправка позиции воспроизведения и сохранение прогресса.
 * Порог интервала сохранения сравнивается на месте вызова через [lastSaveTimeMs].
 */
@Stable
class PlayerProgressReporter internal constructor(
    private val source: () -> PlayerProgressSource,
    private val onEvent: (PlayerState.Event) -> Unit,
) {
    var lastSaveTimeMs: Long = 0L
        private set

    val episodeUrl: String
        get() = source().episodeUrl

    fun notifyPositionChanged(positionMs: Long, durationMs: Long) {
        onEvent(
            PlayerState.Event.PlaybackPositionChanged(
                positionMs = positionMs,
                durationMs = durationMs,
                episodeUrl = source().episodeUrl,
            )
        )
    }

    fun saveProgress(positionMs: Long, durationMs: Long) {
        val snapshot = source().buildProgressSnapshot(
            positionMs = positionMs,
            durationMs = durationMs,
        ) ?: return
        onEvent(PlayerState.Event.SaveProgress(snapshot))
        lastSaveTimeMs = System.currentTimeMillis()
    }
}

@Composable
fun rememberPlayerProgressReporter(
    source: () -> PlayerProgressSource,
    onEvent: (PlayerState.Event) -> Unit,
): PlayerProgressReporter {
    val currentSource = rememberUpdatedState(source)
    val currentOnEvent = rememberUpdatedState(onEvent)
    return remember {
        PlayerProgressReporter(
            source = { currentSource.value.invoke() },
            onEvent = { currentOnEvent.value.invoke(it) },
        )
    }
}
