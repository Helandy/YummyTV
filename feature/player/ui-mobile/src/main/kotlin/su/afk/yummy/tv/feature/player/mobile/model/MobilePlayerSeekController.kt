package su.afk.yummy.tv.feature.player.mobile.model

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.media3.common.Player
import su.afk.yummy.tv.feature.player.common.PlayerProgressReporter
import su.afk.yummy.tv.feature.player.common.PlayerStepSeekToastState
import su.afk.yummy.tv.feature.player.common.model.StepSeekDirection
import su.afk.yummy.tv.feature.player.mobile.utils.isAtMobilePlayerEnd

/** Перемотка мобильного плеера: clamp к длительности, обработка конца эпизода, step-seek. */
@Stable
internal class MobilePlayerSeekController(
    private val player: Player,
    private val fallbackDurationMs: () -> Long,
    private val reporter: PlayerProgressReporter,
    private val stepSeekToast: PlayerStepSeekToastState,
    private val onEpisodeEnd: (positionMs: Long, durationMs: Long) -> Unit,
    private val onBackwardStep: () -> Unit,
) {
    fun seekTo(positionMs: Long) {
        val playerDuration = player.duration.takeIf { it > 0 } ?: fallbackDurationMs()
        val clamped = if (playerDuration > 0) {
            positionMs.coerceIn(0L, playerDuration)
        } else {
            positionMs.coerceAtLeast(0L)
        }
        player.seekTo(clamped)
        if (
            playerDuration > 0L &&
            isAtMobilePlayerEnd(clamped, playerDuration)
        ) {
            onEpisodeEnd(clamped, playerDuration)
        } else {
            reporter.notifyPositionChanged(clamped, playerDuration.coerceAtLeast(0L))
            reporter.saveProgress(clamped, playerDuration)
        }
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
internal fun rememberMobilePlayerSeekController(
    player: Player,
    fallbackDurationMs: () -> Long,
    reporter: PlayerProgressReporter,
    stepSeekToast: PlayerStepSeekToastState,
    onEpisodeEnd: (positionMs: Long, durationMs: Long) -> Unit,
    onBackwardStep: () -> Unit,
): MobilePlayerSeekController {
    val currentFallbackDuration = rememberUpdatedState(fallbackDurationMs)
    val currentOnEpisodeEnd = rememberUpdatedState(onEpisodeEnd)
    val currentOnBackwardStep = rememberUpdatedState(onBackwardStep)
    return remember(player, reporter, stepSeekToast) {
        MobilePlayerSeekController(
            player = player,
            fallbackDurationMs = { currentFallbackDuration.value.invoke() },
            reporter = reporter,
            stepSeekToast = stepSeekToast,
            onEpisodeEnd = { positionMs, durationMs ->
                currentOnEpisodeEnd.value.invoke(positionMs, durationMs)
            },
            onBackwardStep = { currentOnBackwardStep.value.invoke() },
        )
    }
}
