package su.afk.yummy.tv.feature.player.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import su.afk.yummy.tv.feature.player.PlayerState

/**
 * Однократный отчёт о завершении эпизода: notify + save + [PlayerState.Event.EpisodeCompleted].
 * Сбрасывается пересозданием по ключам (contentKey, streamUrl) в remember-фабрике.
 */
@Stable
class PlayerCompletionTracker internal constructor(
    private val reporter: PlayerProgressReporter,
    private val onEvent: (PlayerState.Event) -> Unit,
) {
    var completionReported: Boolean = false
        private set

    fun onEpisodeEnd(positionMs: Long, durationMs: Long) {
        reporter.notifyPositionChanged(positionMs, durationMs)
        reporter.saveProgress(positionMs, durationMs)
        if (!completionReported) {
            completionReported = true
            onEvent(
                PlayerState.Event.EpisodeCompleted(
                    positionMs = positionMs,
                    durationMs = durationMs,
                    episodeUrl = reporter.episodeUrl,
                )
            )
        }
    }
}

@Composable
fun rememberPlayerCompletionTracker(
    contentKey: String,
    streamUrl: String,
    reporter: PlayerProgressReporter,
    onEvent: (PlayerState.Event) -> Unit,
): PlayerCompletionTracker {
    val currentOnEvent = rememberUpdatedState(onEvent)
    return remember(contentKey, streamUrl) {
        PlayerCompletionTracker(
            reporter = reporter,
            onEvent = { currentOnEvent.value.invoke(it) },
        )
    }
}
