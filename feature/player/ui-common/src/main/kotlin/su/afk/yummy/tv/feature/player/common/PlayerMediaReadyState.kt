package su.afk.yummy.tv.feature.player.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.media3.common.Player

/**
 * Становится true после подготовки текущего media item и сбрасывается при смене его ключа.
 */
@Composable
fun rememberPlayerMediaReadyState(
    player: Player?,
    mediaItemKey: String,
): Boolean {
    var isReady by remember(player, mediaItemKey) {
        mutableStateOf(false)
    }

    DisposableEffect(player, mediaItemKey) {
        val activePlayer = player
        if (activePlayer == null) {
            isReady = false
            onDispose { }
        } else {
            fun syncReadyState() {
                isReady = activePlayer.currentMediaItem?.mediaId == mediaItemKey &&
                        activePlayer.playbackState == Player.STATE_READY
            }

            val listener = object : Player.Listener {
                override fun onEvents(player: Player, events: Player.Events) {
                    syncReadyState()
                }
            }
            activePlayer.addListener(listener)
            syncReadyState()
            onDispose { activePlayer.removeListener(listener) }
        }
    }

    return isReady
}
