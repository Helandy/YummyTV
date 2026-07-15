package su.afk.yummy.tv.feature.player.view

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.Player
import su.afk.yummy.tv.feature.player.common.PlayerEndPromptState
import su.afk.yummy.tv.feature.player.common.PlayerProgressReporter
import su.afk.yummy.tv.feature.player.common.downgradedCountdown
import su.afk.yummy.tv.feature.player.common.positionSnapshot
import su.afk.yummy.tv.feature.player.pip.MobilePlayerPipSession

/**
 * Пауза/сохранение прогресса по жизненному циклу с учётом PiP:
 * shouldKeepPlayingOnPause читается ДО сохранения.
 */
@Composable
internal fun MobilePlayerLifecycleEffect(
    player: Player,
    pipSession: MobilePlayerPipSession,
    reporter: PlayerProgressReporter,
    resumeAfterPause: MutableState<Boolean>,
    fallbackDurationMs: () -> Long,
    wantsPlay: () -> Boolean,
    promptState: () -> PlayerEndPromptState,
    onPromptStateChange: (PlayerEndPromptState) -> Unit,
) {
    val currentFallbackDuration by rememberUpdatedState(fallbackDurationMs)
    val currentWantsPlay by rememberUpdatedState(wantsPlay)
    val currentPromptState by rememberUpdatedState(promptState)
    val currentOnPromptStateChange by rememberUpdatedState(onPromptStateChange)

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, player) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> {
                    val prompt = currentPromptState()
                    val downgraded = prompt.downgradedCountdown()
                    if (downgraded !== prompt) currentOnPromptStateChange(downgraded)
                    val keepPlayingInPip = pipSession.shouldKeepPlayingOnPause()
                    resumeAfterPause.value = currentWantsPlay() && !keepPlayingInPip
                    val snapshot = player.positionSnapshot(currentFallbackDuration())
                    reporter.notifyPositionChanged(snapshot.positionMs, snapshot.durationMs)
                    reporter.saveProgress(snapshot.positionMs, snapshot.durationMs)
                    if (!keepPlayingInPip) {
                        player.pause()
                    }
                }

                Lifecycle.Event.ON_RESUME -> if (resumeAfterPause.value) player.play()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
}
