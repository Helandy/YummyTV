package su.afk.yummy.tv.feature.player.view.player

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.media3.common.Player
import kotlinx.coroutines.delay
import su.afk.yummy.tv.feature.player.common.PlayerProgressReporter
import su.afk.yummy.tv.feature.player.common.calculateBufferedProgress
import su.afk.yummy.tv.feature.player.model.TvPlaybackProgressState
import kotlin.time.Duration.Companion.milliseconds

/**
 * Цикл 500ms: позиция (с защитой после seek), длительность, буферизация,
 * notify раз в секунду и сохранение каждые 10 секунд.
 */
@Composable
internal fun TvPlayerProgressPollingEffect(
    player: Player,
    progress: TvPlaybackProgressState,
    reporter: PlayerProgressReporter,
    episodeKey: () -> String,
    onBufferedProgressChange: (Float) -> Unit,
) {
    val currentEpisodeKey by rememberUpdatedState(episodeKey)
    val currentOnBufferedProgressChange by rememberUpdatedState(onBufferedProgressChange)

    LaunchedEffect(player) {
        while (true) {
            val sinceSeek = System.currentTimeMillis() - progress.lastSeekTimeMs
            if (!progress.isSeeking && sinceSeek > 1_000L) {
                progress.currentPosition = player.currentPosition.coerceAtLeast(0)
            }
            val dur = player.duration
            if (dur > 0) progress.duration = dur
            currentOnBufferedProgressChange(
                calculateBufferedProgress(
                    bufferedPosition = player.bufferedPosition,
                    currentPosition = progress.currentPosition,
                    duration = progress.duration,
                )
            )
            val now = System.currentTimeMillis()
            if (!progress.isSeeking && progress.duration > 0 &&
                now - progress.lastPositionNotifyTimeMs >= 1_000L
            ) {
                reporter.notifyPositionChanged(progress.currentPosition, progress.duration)
                progress.lastPositionNotifyTimeMs = now
            }
            if (currentEpisodeKey().isNotBlank() && progress.duration > 0 &&
                now - reporter.lastSaveTimeMs > 10_000L
            ) {
                reporter.saveProgress(progress.currentPosition, progress.duration)
            }
            delay(500.milliseconds)
        }
    }
}
