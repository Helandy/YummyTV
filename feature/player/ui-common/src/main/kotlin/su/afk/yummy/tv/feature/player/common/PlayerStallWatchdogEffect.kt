package su.afk.yummy.tv.feature.player.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.media3.common.Player
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import su.afk.yummy.tv.feature.player.PlayerState

/**
 * Сторож зависшей буферизации для ТВ и мобилки. Загрузчик ExoPlayer молча ретраит упавшие
 * загрузки до 20 раз (~1,5 мин), и всё это время при пустом буфере юзер видит голый спиннер.
 * Если буферизация при запрошенном воспроизведении длится дольше [STALL_TIMEOUT_MS], шлём
 * [PlayerState.Event.PlaybackStalled] — поведение источника перезапрашивает свежую ссылку.
 * Срабатывает один раз на каждый заход в буферизацию.
 */
@Composable
fun PlayerStallWatchdogEffect(
    player: Player,
    onEvent: (PlayerState.Event) -> Unit,
) {
    val currentOnEvent by rememberUpdatedState(onEvent)
    val scope = rememberCoroutineScope()

    DisposableEffect(player) {
        var watchdog: Job? = null

        fun update() {
            val stalling = player.playbackState == Player.STATE_BUFFERING && player.playWhenReady
            if (!stalling) {
                watchdog?.cancel()
                watchdog = null
                return
            }
            if (watchdog?.isActive == true) return
            watchdog = scope.launch {
                delay(STALL_TIMEOUT_MS)
                currentOnEvent(PlayerState.Event.PlaybackStalled)
            }
        }

        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) = update()

            override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) = update()
        }
        player.addListener(listener)
        update()
        onDispose {
            watchdog?.cancel()
            player.removeListener(listener)
        }
    }
}

/** Как у Alloha (RECOVERY_HINT_DELAY_MS): переждать обычную подгрузку, но не полторы минуты. */
private const val STALL_TIMEOUT_MS = 15_000L
