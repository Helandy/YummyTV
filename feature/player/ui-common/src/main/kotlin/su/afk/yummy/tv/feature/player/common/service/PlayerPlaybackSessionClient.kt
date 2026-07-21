package su.afk.yummy.tv.feature.player.common.service

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken

@Stable
class PlayerPlaybackSessionClient internal constructor(context: Context) {
    private val applicationContext = context.applicationContext
    private val playerState = mutableStateOf<MediaController?>(null)
    private var stopRequested = false
    private var stoppedPlayer: MediaController? = null
    private var serviceStoppedWithoutPlayer = false

    val player: MediaController?
        get() = playerState.value

    /** Останавливает playback-сессию и сервис; повторные вызовы безопасны. */
    fun stopPlaybackAndService() {
        stopRequested = true
        val currentPlayer = playerState.value
        if (currentPlayer != null && stoppedPlayer !== currentPlayer) {
            runCatching { currentPlayer.pause() }
            runCatching { currentPlayer.clearMediaItems() }
            stoppedPlayer = currentPlayer
            stopService()
        } else if (currentPlayer == null && !serviceStoppedWithoutPlayer) {
            serviceStoppedWithoutPlayer = true
            stopService()
        }
    }

    internal fun connect(player: MediaController) {
        playerState.value = player
        if (stopRequested) stopPlaybackAndService()
    }

    internal fun disconnect() {
        playerState.value = null
    }

    private fun stopService() {
        runCatching {
            applicationContext.stopService(
                Intent(applicationContext, PlayerMediaSessionService::class.java)
            )
        }
    }
}

@Composable
fun rememberPlayerPlaybackSessionClient(): PlayerPlaybackSessionClient {
    val context = LocalContext.current
    val client = remember(context) { PlayerPlaybackSessionClient(context) }
    DisposableEffect(context, client) {
        var active = true
        val token =
            SessionToken(context, ComponentName(context, PlayerMediaSessionService::class.java))
        val future = MediaController.Builder(context, token).buildAsync()
        future.addListener(
            {
                if (active) {
                    runCatching { future.get() }.getOrNull()?.let(client::connect)
                }
            },
            ContextCompat.getMainExecutor(context),
        )
        onDispose {
            active = false
            client.disconnect()
            MediaController.releaseFuture(future)
        }
    }
    return client
}
