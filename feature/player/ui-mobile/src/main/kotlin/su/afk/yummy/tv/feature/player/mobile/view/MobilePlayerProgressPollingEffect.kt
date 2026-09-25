package su.afk.yummy.tv.feature.player.mobile.view

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.media3.common.Player
import kotlinx.coroutines.delay
import su.afk.yummy.tv.feature.player.common.PlayerProgressReporter
import su.afk.yummy.tv.feature.player.common.model.PlayerPlaybackProgressState
import su.afk.yummy.tv.feature.player.common.utils.PLAYER_PROGRESS_POLL_INTERVAL
import su.afk.yummy.tv.feature.player.common.utils.updateBufferedProgress

/** Секундный цикл: notify позиции, буферизация и сохранение прогресса каждые 10 секунд. */
@Composable
internal fun MobilePlayerProgressPollingEffect(
    player: Player,
    episodeKey: String,
    isMediaReady: Boolean,
    reporter: PlayerProgressReporter,
    progress: PlayerPlaybackProgressState,
) {
    // Держатель пересоздаётся на новом стриме той же серии (смена качества), а цикл — нет.
    val currentProgress by rememberUpdatedState(progress)

    LaunchedEffect(player, episodeKey, isMediaReady) {
        while (true) {
            val state = currentProgress
            var position = state.currentPosition
            var dur = state.duration
            if (!state.isSeeking) {
                position = player.currentPosition.coerceAtLeast(0)
                dur = player.duration.takeIf { it > 0 } ?: 0L
                reporter.notifyPositionChanged(position, dur)
            }
            state.updateBufferedProgress(player, currentPositionMs = position, durationMs = dur)
            val now = System.currentTimeMillis()
            if (dur > 0 && now - reporter.lastSaveTimeMs >= 10_000L) {
                reporter.saveProgress(position, dur)
            }
            delay(PLAYER_PROGRESS_POLL_INTERVAL)
        }
    }
}
