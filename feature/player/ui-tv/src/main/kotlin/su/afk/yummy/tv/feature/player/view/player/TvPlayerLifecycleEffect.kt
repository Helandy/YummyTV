package su.afk.yummy.tv.feature.player.view.player

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.Player
import su.afk.yummy.tv.feature.player.common.PlayerProgressReporter
import su.afk.yummy.tv.feature.player.common.downgradedCountdown
import su.afk.yummy.tv.feature.player.common.positionSnapshot
import su.afk.yummy.tv.feature.player.common.service.PlayerMediaSessionService
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

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, player) {
        var stopped = false
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

                Lifecycle.Event.ON_STOP -> {
                    stopped = true
                    releaseTvPlaybackResources(context, player)
                }

                Lifecycle.Event.ON_RESUME -> if (!stopped && currentWantsPlay()) player.play()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
}

internal fun releaseTvPlaybackResources(context: Context, player: Player) {
    runCatching { player.pause() }
    runCatching { player.clearMediaItems() }
    runCatching { context.stopService(Intent(context, PlayerMediaSessionService::class.java)) }
}
