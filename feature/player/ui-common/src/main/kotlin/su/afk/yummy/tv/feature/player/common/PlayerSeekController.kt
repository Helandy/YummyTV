package su.afk.yummy.tv.feature.player.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.media3.common.Player
import su.afk.yummy.tv.feature.player.common.model.PlayerPlaybackProgressState
import su.afk.yummy.tv.feature.player.common.model.StepSeekDirection
import su.afk.yummy.tv.feature.player.common.utils.isAtPlayerEnd

/**
 * Перемотка плеера для ТВ и мобилки: clamp к длительности, конец эпизода при перемотке в конец,
 * сброс сценария конца при уходе от него назад, step-seek.
 */
@Stable
class PlayerSeekController internal constructor(
    private val player: Player,
    private val progress: () -> PlayerPlaybackProgressState,
    private val reporter: PlayerProgressReporter,
    private val stepSeekToast: PlayerStepSeekToastState,
    private val onEpisodeEnd: (positionMs: Long, durationMs: Long) -> Unit,
    private val onLeftEnd: () -> Unit,
) {
    fun seekTo(positionMs: Long) {
        val state = progress()
        // Длительность плеера надёжнее опрошенной: до первого тика поллинга её там ещё нет.
        val duration = player.duration.takeIf { it > 0 } ?: state.duration.coerceAtLeast(0L)
        val clamped = if (duration > 0) {
            positionMs.coerceIn(0L, duration)
        } else {
            positionMs.coerceAtLeast(0L)
        }
        val isBackward = clamped < player.currentPosition
        player.seekTo(clamped)
        state.currentPosition = clamped
        state.lastSeekTimeMs = System.currentTimeMillis()
        if (isAtPlayerEnd(clamped, duration)) {
            onEpisodeEnd(clamped, duration)
        } else {
            if (isBackward) onLeftEnd()
            reporter.notifyPositionChanged(clamped, duration)
            reporter.saveProgress(clamped, duration)
        }
    }

    fun stepSeek(direction: StepSeekDirection) {
        val offset = stepSeekToast.nextOffsetMs(direction, System.currentTimeMillis())
        seekTo(player.currentPosition + offset)
        stepSeekToast.showToast(direction)
    }
}

@Composable
fun rememberPlayerSeekController(
    player: Player,
    progress: PlayerPlaybackProgressState,
    reporter: PlayerProgressReporter,
    stepSeekToast: PlayerStepSeekToastState,
    onEpisodeEnd: (positionMs: Long, durationMs: Long) -> Unit,
    onLeftEnd: () -> Unit,
): PlayerSeekController {
    val currentProgress by rememberUpdatedState(progress)
    val currentOnEpisodeEnd by rememberUpdatedState(onEpisodeEnd)
    val currentOnLeftEnd by rememberUpdatedState(onLeftEnd)
    return remember(player, reporter, stepSeekToast) {
        PlayerSeekController(
            player = player,
            progress = { currentProgress },
            reporter = reporter,
            stepSeekToast = stepSeekToast,
            onEpisodeEnd = { positionMs, durationMs -> currentOnEpisodeEnd(positionMs, durationMs) },
            onLeftEnd = { currentOnLeftEnd() },
        )
    }
}
