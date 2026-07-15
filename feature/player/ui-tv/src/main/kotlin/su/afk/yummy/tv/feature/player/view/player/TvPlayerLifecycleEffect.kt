package su.afk.yummy.tv.feature.player.view.player

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.Player
import su.afk.yummy.tv.feature.player.common.PlayerProgressReporter
import su.afk.yummy.tv.feature.player.common.downgradedCountdown
import su.afk.yummy.tv.feature.player.common.positionSnapshot
import su.afk.yummy.tv.feature.player.model.TvPlayerPromptsState

/** Пауза/сохранение прогресса по жизненному циклу; возобновление по wantsPlay. */
@Composable
internal fun TvPlayerLifecycleEffect(
    player: Player,
    reporter: PlayerProgressReporter,
    prompts: TvPlayerPromptsState,
    fallbackDurationMs: () -> Long,
    wantsPlay: () -> Boolean,
) {
    val currentFallbackDuration by rememberUpdatedState(fallbackDurationMs)
    val currentWantsPlay by rememberUpdatedState(wantsPlay)

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, player) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> {
                    val prompt = prompts.nextEpisodePrompt
                    val downgraded = prompt.downgradedCountdown()
                    if (downgraded !== prompt) prompts.nextEpisodePrompt = downgraded
                    val snapshot = player.positionSnapshot(currentFallbackDuration())
                    reporter.notifyPositionChanged(snapshot.positionMs, snapshot.durationMs)
                    reporter.saveProgress(snapshot.positionMs, snapshot.durationMs)
                    player.pause()
                }

                Lifecycle.Event.ON_RESUME -> if (currentWantsPlay()) player.play()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
}
