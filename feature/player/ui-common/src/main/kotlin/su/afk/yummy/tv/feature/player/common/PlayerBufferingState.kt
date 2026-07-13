package su.afk.yummy.tv.feature.player.common

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.media3.common.Player

/**
 * Возвращает согласованный snapshot видимой буферизации после полного batch событий Media3.
 * Фоновую загрузку поставленного на паузу видео намеренно не считаем видимой буферизацией.
 */
@Composable
fun rememberPlayerBufferingState(player: Player?): Boolean {
    var isBuffering by remember(player) {
        mutableStateOf(player?.isVisibleBuffering() == true)
    }

    DisposableEffect(player) {
        val activePlayer = player
        if (activePlayer == null) {
            isBuffering = false
            onDispose { }
        } else {
            fun syncBufferingState(playerSnapshot: Player) {
                val next = playerSnapshot.isVisibleBuffering()
                if (next != isBuffering) {
                    Log.d(
                        BUFFERING_LOG_TAG,
                        "visible=$next playbackState=${playerSnapshot.playbackState.name()} " +
                                "playWhenReady=${playerSnapshot.playWhenReady}",
                    )
                    isBuffering = next
                }
            }

            val listener = object : Player.Listener {
                override fun onEvents(player: Player, events: Player.Events) {
                    if (
                        events.contains(Player.EVENT_PLAYBACK_STATE_CHANGED) ||
                        events.contains(Player.EVENT_PLAY_WHEN_READY_CHANGED)
                    ) {
                        syncBufferingState(player)
                    }
                }
            }
            activePlayer.addListener(listener)
            syncBufferingState(activePlayer)
            onDispose { activePlayer.removeListener(listener) }
        }
    }

    return isBuffering
}

private fun Player.isVisibleBuffering(): Boolean =
    playbackState == Player.STATE_BUFFERING && playWhenReady

private fun Int.name(): String = when (this) {
    Player.STATE_IDLE -> "IDLE"
    Player.STATE_BUFFERING -> "BUFFERING"
    Player.STATE_READY -> "READY"
    Player.STATE_ENDED -> "ENDED"
    else -> toString()
}

private const val BUFFERING_LOG_TAG = "PlayerBuffering"
