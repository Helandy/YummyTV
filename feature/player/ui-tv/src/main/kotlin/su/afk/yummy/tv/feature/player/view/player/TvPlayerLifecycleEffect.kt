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
import su.afk.yummy.tv.feature.player.common.service.PlayerPlaybackSessionClient
import su.afk.yummy.tv.feature.player.common.utils.downgradedCountdown
import su.afk.yummy.tv.feature.player.common.utils.positionSnapshot
import su.afk.yummy.tv.feature.player.model.TvPlayerPromptsState

/** Сохраняет прогресс и единожды выгружает TV playback-сессию без PiP. */
@Composable
internal fun TvPlayerLifecycleEffect(
    player: Player?,
    playbackSession: PlayerPlaybackSessionClient,
    reporter: PlayerProgressReporter,
    prompts: TvPlayerPromptsState,
    fallbackDurationMs: () -> Long,
    wantsPlay: () -> Boolean,
) {
    val currentFallbackDuration by rememberUpdatedState(fallbackDurationMs)
    val currentWantsPlay by rememberUpdatedState(wantsPlay)
    val currentPlayer by rememberUpdatedState(player)

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, playbackSession) {
        var released = false

        fun saveProgressAndPause() {
            val activePlayer = currentPlayer ?: return
            if (activePlayer.mediaItemCount > 0) {
                val snapshot = activePlayer.positionSnapshot(currentFallbackDuration())
                reporter.notifyPositionChanged(snapshot.positionMs, snapshot.durationMs)
                reporter.saveProgress(snapshot.positionMs, snapshot.durationMs)
            }
            activePlayer.pause()
        }

        fun releasePlayback() {
            if (released) return
            released = true
            saveProgressAndPause()
            playbackSession.stopPlaybackAndService()
        }

        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> {
                    val prompt = prompts.nextEpisodePrompt
                    val downgraded = prompt.downgradedCountdown()
                    if (downgraded !== prompt) prompts.nextEpisodePrompt = downgraded
                    if (!released) saveProgressAndPause()
                }

                Lifecycle.Event.ON_STOP -> releasePlayback()

                Lifecycle.Event.ON_RESUME -> if (!released && currentWantsPlay()) {
                    currentPlayer?.play()
                }

                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            releasePlayback()
        }
    }
}
