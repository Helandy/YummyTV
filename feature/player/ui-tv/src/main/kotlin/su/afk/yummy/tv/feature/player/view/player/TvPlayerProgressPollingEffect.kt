package su.afk.yummy.tv.feature.player.view.player

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.media3.common.Player
import kotlinx.coroutines.delay
import su.afk.yummy.tv.feature.player.common.PlayerProgressReporter
import su.afk.yummy.tv.feature.player.common.utils.calculateBufferedProgress
import su.afk.yummy.tv.feature.player.common.utils.isAtPlayerEnd
import kotlin.time.Duration.Companion.milliseconds
import su.afk.yummy.tv.feature.player.common.model.PlayerPlaybackProgressState

/**
 * Цикл 500ms: позиция (с защитой после seek), длительность, буферизация,
 * notify раз в секунду и сохранение каждые 10 секунд.
 *
 * Здесь же страховка конца эпизода: часть потоков не доигрывает до duration и не даёт
 * STATE_ENDED, поэтому конец ловим ещё и по позиции.
 */
@Composable
internal fun TvPlayerProgressPollingEffect(
    player: Player,
    progress: PlayerPlaybackProgressState,
    reporter: PlayerProgressReporter,
    episodeKey: () -> String,
    onBufferedProgressChange: (Float) -> Unit,
    onPositionAtEnd: (positionMs: Long, durationMs: Long) -> Unit,
) {
    val currentEpisodeKey by rememberUpdatedState(episodeKey)
    val currentOnBufferedProgressChange by rememberUpdatedState(onBufferedProgressChange)
    val currentOnPositionAtEnd by rememberUpdatedState(onPositionAtEnd)

    LaunchedEffect(player) {
        // Серию, открытую сразу с конечной позиции, концом не считаем: сначала должна быть
        // позиция вне зоны конца, иначе промпт выскочит на старте
        var sawPositionBeforeEnd = false
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
            if (!progress.isSeeking && progress.duration > 0) {
                if (isAtPlayerEnd(progress.currentPosition, progress.duration)) {
                    if (sawPositionBeforeEnd) {
                        currentOnPositionAtEnd(progress.currentPosition, progress.duration)
                    }
                } else {
                    sawPositionBeforeEnd = true
                }
            }
            delay(500.milliseconds)
        }
    }
}
