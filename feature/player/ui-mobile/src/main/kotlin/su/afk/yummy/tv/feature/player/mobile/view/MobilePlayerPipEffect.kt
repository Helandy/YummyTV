package su.afk.yummy.tv.feature.player.mobile.view

import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import su.afk.yummy.tv.feature.player.common.PlayerSeekController
import su.afk.yummy.tv.feature.player.mobile.pip.MobilePlayerPipSession
import su.afk.yummy.tv.feature.player.mobile.pip.model.MobilePlayerPipCallbacks
import su.afk.yummy.tv.feature.player.mobile.utils.MOBILE_PLAYER_PIP_SEEK_STEP_MS

/**
 * PiP-часть мобильного плеера: состояние play/pause и соотношение сторон для окна PiP и его
 * кнопки. Общая логика слушателя — в PlayerListenerEffect.
 */
@Composable
internal fun MobilePlayerPipEffect(
    player: Player,
    activity: Activity?,
    pipSession: MobilePlayerPipSession,
    seekController: PlayerSeekController,
) {
    val currentSeekController by rememberUpdatedState(seekController)

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
                pipSession.setPlaying(playWhenReady, activity)
            }

            override fun onVideoSizeChanged(videoSize: VideoSize) {
                pipSession.setAspectRatio(videoSize.width, videoSize.height)
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
        onDispose {
            pipSession.setPlaying(false, activity)
            pipSession.setCallbacks(null)
            player.removeListener(listener)
        }
    }
}
