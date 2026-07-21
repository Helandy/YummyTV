package su.afk.yummy.tv.feature.player.mobile.view

import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import su.afk.yummy.tv.feature.player.PlayerState
import su.afk.yummy.tv.feature.player.common.PlayerProgressReporter
import su.afk.yummy.tv.feature.player.common.PlayerStepSeekToastState
import su.afk.yummy.tv.feature.player.common.logPlaybackError
import su.afk.yummy.tv.feature.player.common.toPlaybackErrorEvent
import su.afk.yummy.tv.feature.player.common.utils.positionSnapshot
import su.afk.yummy.tv.feature.player.mobile.model.MobilePlayerOverlayController
import su.afk.yummy.tv.feature.player.mobile.model.MobilePlayerSeekController
import su.afk.yummy.tv.feature.player.mobile.pip.MobilePlayerPipSession
import su.afk.yummy.tv.feature.player.mobile.pip.model.MobilePlayerPipCallbacks
import su.afk.yummy.tv.feature.player.mobile.utils.MOBILE_PLAYER_PIP_SEEK_STEP_MS

/**
 * Player.Listener + PiP-колбэки мобильного плеера.
 * Порядок dispose важен: cancel jobs -> setPlaying(false) -> setCallbacks(null) ->
 * notify/save -> removeListener -> условный clear/stop.
 */
@Composable
internal fun MobilePlayerListenerEffect(
    player: Player,
    activity: Activity?,
    pipSession: MobilePlayerPipSession,
    reporter: PlayerProgressReporter,
    overlay: MobilePlayerOverlayController,
    stepSeekToast: PlayerStepSeekToastState,
    seekController: MobilePlayerSeekController,
    fallbackDurationMs: () -> Long,
    wantsPlay: () -> Boolean,
    onWantsPlayChanged: (Boolean) -> Unit,
    onEpisodeEnd: (positionMs: Long, durationMs: Long) -> Unit,
    onEvent: (PlayerState.Event) -> Unit,
) {
    val currentFallbackDuration by rememberUpdatedState(fallbackDurationMs)
    val currentWantsPlay by rememberUpdatedState(wantsPlay)
    val currentOnWantsPlayChanged by rememberUpdatedState(onWantsPlayChanged)
    val currentOnEpisodeEnd by rememberUpdatedState(onEpisodeEnd)
    val currentOnEvent by rememberUpdatedState(onEvent)
    val currentSeekController by rememberUpdatedState(seekController)
    val currentStepSeekToast by rememberUpdatedState(stepSeekToast)

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
                currentOnWantsPlayChanged(playWhenReady)
                pipSession.setPlaying(playWhenReady, activity)
                if (playWhenReady) overlay.scheduleHide() else overlay.cancelHide()
            }

            override fun onPlayerError(error: PlaybackException) {
                val position = player.currentPosition.coerceAtLeast(0L)
                logPlaybackError("Mobile", position, error)
                currentOnEvent(error.toPlaybackErrorEvent(position))
            }

            override fun onVideoSizeChanged(videoSize: VideoSize) {
                pipSession.setAspectRatio(videoSize.width, videoSize.height)
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY) {
                    currentOnEvent(PlayerState.Event.PlaybackReady)
                }
                if (playbackState == Player.STATE_ENDED) {
                    val snapshot = player.positionSnapshot(currentFallbackDuration())
                    currentOnEpisodeEnd(snapshot.positionMs, snapshot.durationMs)
                }
            }
        }
        player.addListener(listener)
        pipSession.setPlaying(player.playWhenReady, activity)
        pipSession.setCallbacks(
            MobilePlayerPipCallbacks(
                onSeekBackward = {
                    currentSeekController.seekTo(
                        player.currentPosition - MOBILE_PLAYER_PIP_SEEK_STEP_MS
                    )
                },
                onPlayPause = {
                    if (player.playWhenReady) player.pause() else player.play()
                },
                onSeekForward = {
                    currentSeekController.seekTo(
                        player.currentPosition + MOBILE_PLAYER_PIP_SEEK_STEP_MS
                    )
                },
            )
        )
        if (currentWantsPlay()) overlay.scheduleHide()
        onDispose {
            overlay.cancelHide()
            currentStepSeekToast.cancel()
            pipSession.setPlaying(false, activity)
            pipSession.setCallbacks(null)
            val snapshot = player.positionSnapshot(currentFallbackDuration())
            reporter.notifyPositionChanged(snapshot.positionMs, snapshot.durationMs)
            reporter.saveProgress(snapshot.positionMs, snapshot.durationMs)
            player.removeListener(listener)
            if (!pipSession.shouldKeepPlayingOnPause()) {
                player.clearMediaItems()
                player.stop()
            }
        }
    }
}
