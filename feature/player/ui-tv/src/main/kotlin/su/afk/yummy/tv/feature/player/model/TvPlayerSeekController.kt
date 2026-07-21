package su.afk.yummy.tv.feature.player.model

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.media3.common.Player
import su.afk.yummy.tv.feature.player.common.PlayerProgressReporter
import su.afk.yummy.tv.feature.player.common.PlayerStepSeekToastState
import su.afk.yummy.tv.feature.player.common.model.StepSeekDirection

/** Перемотка TV-плеера: clamp к длительности, обновление локальной позиции, step-seek. */
@Stable
internal class TvPlayerSeekController(
    private val player: Player,
    private val progress: TvPlaybackProgressState,
    private val reporter: PlayerProgressReporter,
    private val stepSeekToast: PlayerStepSeekToastState,
    private val onBackwardStep: () -> Unit,
) {
    fun seekTo(positionMs: Long) {
        val playerDuration = progress.duration.coerceAtLeast(0L)
        val clamped =
            if (playerDuration > 0) {
                positionMs.coerceIn(0L, playerDuration)
            } else {
                positionMs.coerceAtLeast(0)
            }
        player.seekTo(clamped)
        progress.currentPosition = clamped
        progress.lastSeekTimeMs = System.currentTimeMillis()
        reporter.notifyPositionChanged(clamped, playerDuration)
        reporter.saveProgress(clamped, progress.duration)
    }

    fun stepSeek(direction: StepSeekDirection) {
        if (direction == StepSeekDirection.Backward) {
            onBackwardStep()
        }
        val offset = stepSeekToast.nextOffsetMs(direction, System.currentTimeMillis())
        seekTo(player.currentPosition + offset)
        stepSeekToast.showToast(direction)
    }
}

@Composable
internal fun rememberTvPlayerSeekController(
    player: Player,
    progress: TvPlaybackProgressState,
    reporter: PlayerProgressReporter,
    stepSeekToast: PlayerStepSeekToastState,
    onBackwardStep: () -> Unit,
): TvPlayerSeekController {
    val currentOnBackwardStep = rememberUpdatedState(onBackwardStep)
    return remember(player, progress, reporter, stepSeekToast) {
        TvPlayerSeekController(
            player = player,
            progress = progress,
            reporter = reporter,
            stepSeekToast = stepSeekToast,
            onBackwardStep = { currentOnBackwardStep.value.invoke() },
        )
    }
}
