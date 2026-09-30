package su.afk.yummy.tv.feature.player.common.service

import android.content.ComponentName
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import java.util.concurrent.Executor

@Stable
class PlayerPlaybackSessionClient internal constructor() {
    private val playerState = mutableStateOf<MediaController?>(null)
    private var stopRequested = false
    private var releaseAfterStop = false
    private var stoppedPlayer: MediaController? = null

    // Отпускает подключение этого клиента к сервису; ставит rememberPlayerPlaybackSessionClient.
    internal var connectionRelease: (() -> Unit)? = null

    // Отпускание трогает Compose-состояние (player), поэтому только на главном потоке.
    private val mainExecutor = Executor { Handler(Looper.getMainLooper()).post(it) }

    val player: MediaController?
        get() = playerState.value

    /**
     * Останавливает playback-сессию и сервис; повторные вызовы безопасны.
     *
     * Сервис не гасится через Context.stopService: команда едет по тому же IPC-каналу, что pause и
     * clearMediaItems, поэтому порядок гарантирован, а завершение выполняет сам сервис
     * (pauseAllPlayersAndStopSelf) уже после выхода из foreground. Внешний снос foreground-сервиса
     * система расценивает как startForegroundService() без startForeground() и убивает процесс.
     *
     * @param releaseConnection после ответа сервиса на команду отпустить и само подключение. Пока
     *   контроллер привязан, сервис после stopSelf() живёт дальше, а в свёрнутом приложении экран
     *   плеера (и его подключение) не уходит из композиции до возврата пользователя.
     */
    fun stopPlaybackAndService(releaseConnection: Boolean = false) {
        stopRequested = true
        releaseAfterStop = releaseAfterStop || releaseConnection
        val currentPlayer = playerState.value ?: return
        if (stoppedPlayer === currentPlayer) return
        stoppedPlayer = currentPlayer
        runCatching { currentPlayer.pause() }
        runCatching { currentPlayer.clearMediaItems() }
        val result = runCatching {
            currentPlayer.sendCustomCommand(PlayerSessionCommands.STOP_SERVICE, Bundle.EMPTY)
        }.getOrNull()
        if (releaseAfterStop) {
            // Сначала сервис должен получить команду и вызвать stopSelf(), иначе отвязка
            // последнего контроллера уничтожит его раньше, чем он выйдет из foreground.
            result?.addListener(
                { connectionRelease?.invoke() },
                mainExecutor,
            ) ?: connectionRelease?.invoke()
        }
    }

    internal fun connect(player: MediaController) {
        playerState.value = player
        if (stopRequested) stopPlaybackAndService(releaseAfterStop)
    }

    internal fun disconnect() {
        playerState.value = null
    }
}

@Composable
fun rememberPlayerPlaybackSessionClient(): PlayerPlaybackSessionClient {
    val context = LocalContext.current
    val client = remember(context) { PlayerPlaybackSessionClient() }
    DisposableEffect(context, client) {
        var active = true
        var released = false
        // Только Application: release() отвязывается отложенно (до 30 с), а bind'ы Activity-контекста
        // система снимает сама при её уничтожении — повторный unbindService падает с «Service not registered».
        val appContext = context.applicationContext
        val token =
            SessionToken(appContext, ComponentName(appContext, PlayerMediaSessionService::class.java))
        val future = MediaController.Builder(appContext, token).buildAsync()
        future.addListener(
            {
                if (active) {
                    runCatching { future.get() }.getOrNull()?.let(client::connect)
                }
            },
            ContextCompat.getMainExecutor(context),
        )
        fun release() {
            if (released) return
            released = true
            active = false
            client.disconnect()
            MediaController.releaseFuture(future)
        }
        client.connectionRelease = ::release
        onDispose {
            client.connectionRelease = null
            release()
        }
    }
    return client
}
