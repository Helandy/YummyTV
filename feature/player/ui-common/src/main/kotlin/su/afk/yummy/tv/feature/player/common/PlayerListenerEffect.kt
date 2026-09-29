package su.afk.yummy.tv.feature.player.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import su.afk.yummy.tv.feature.player.PlayerState
import su.afk.yummy.tv.feature.player.common.utils.positionSnapshot

/**
 * Player.Listener, общий для ТВ и мобилки: play/pause ↔ wantsPlay и автоскрытие контролов,
 * готовность потока, конец эпизода по STATE_ENDED и ошибки воспроизведения.
 * Платформенные подписки (PiP на мобилке) живут в своих эффектах.
 *
 * @param autoHide запланировать (true) или отменить (false) автоскрытие контролов.
 */
@Composable
fun PlayerListenerEffect(
    player: Player,
    skipUi: PlayerSkipUiState,
    stepSeekToast: PlayerStepSeekToastState,
    fallbackDurationMs: () -> Long,
    wantsPlay: () -> Boolean,
    onWantsPlayChanged: (Boolean) -> Unit,
    autoHide: (Boolean) -> Unit,
    onEpisodeEnd: (positionMs: Long, durationMs: Long) -> Unit,
    onEvent: (PlayerState.Event) -> Unit,
) {
    val currentFallbackDuration by rememberUpdatedState(fallbackDurationMs)
    val currentWantsPlay by rememberUpdatedState(wantsPlay)
    val currentOnWantsPlayChanged by rememberUpdatedState(onWantsPlayChanged)
    val currentAutoHide by rememberUpdatedState(autoHide)
    val currentOnEpisodeEnd by rememberUpdatedState(onEpisodeEnd)
    val currentOnEvent by rememberUpdatedState(onEvent)
    val currentSkipUi by rememberUpdatedState(skipUi)
    val currentStepSeekToast by rememberUpdatedState(stepSeekToast)

    DisposableEffect(player) {
        player.playWhenReady = currentWantsPlay()
        val listener = object : Player.Listener {
            override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
                currentOnWantsPlayChanged(playWhenReady)
                currentAutoHide(playWhenReady)
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

            override fun onPlayerError(error: PlaybackException) {
                val position = player.currentPosition.coerceAtLeast(0L)
                currentOnEvent(error.toPlaybackErrorEvent(position))
            }
        }
        player.addListener(listener)
        currentAutoHide(currentWantsPlay())
        onDispose {
            currentAutoHide(false)
            currentSkipUi.cancel()
            currentStepSeekToast.cancel()
            player.removeListener(listener)
        }
    }
}
