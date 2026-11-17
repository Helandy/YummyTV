package su.afk.yummy.tv.feature.player.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import su.afk.yummy.tv.feature.player.PlayerState
import su.afk.yummy.tv.feature.player.utils.UpscaleOverloadDetector

/**
 * Следит за пропущенными кадрами при включённом апскейле (Deblur) и один раз шлёт
 * [PlayerState.Event.UpscaleOverloaded], когда устройство не успевает: картинка отстаёт от звука.
 * Перемотка и смена потока заново запускают прогрев детектора.
 */
@Composable
fun PlayerUpscaleOverloadEffect(
    player: Player,
    enabled: Boolean,
    onEvent: (PlayerState.Event) -> Unit,
) {
    if (!enabled) return
    val signals = rememberPlayerQualitySignals()
    val currentOnEvent by rememberUpdatedState(onEvent)
    val detector = remember(player) { UpscaleOverloadDetector().apply { reset(nowMs()) } }

    DisposableEffect(player, detector) {
        val listener = object : Player.Listener {
            override fun onPositionDiscontinuity(
                oldPosition: Player.PositionInfo,
                newPosition: Player.PositionInfo,
                reason: Int,
            ) {
                detector.reset(nowMs())
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                detector.reset(nowMs())
            }
        }
        player.addListener(listener)
        onDispose { player.removeListener(listener) }
    }

    LaunchedEffect(player, detector) {
        signals.events.collect { signal ->
            if (signal is PlayerQualitySignals.Signal.DroppedFrames &&
                detector.onDroppedFrames(signal.count, nowMs())
            ) {
                currentOnEvent(PlayerState.Event.UpscaleOverloaded)
            }
        }
    }
}

private fun nowMs(): Long = android.os.SystemClock.elapsedRealtime()
