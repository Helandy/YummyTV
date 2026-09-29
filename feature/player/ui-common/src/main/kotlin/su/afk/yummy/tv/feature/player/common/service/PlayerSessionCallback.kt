package su.afk.yummy.tv.feature.player.common.service

import android.os.Bundle
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaSession
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import su.afk.yummy.tv.core.analytics.api.AnalyticsTracker
import su.afk.yummy.tv.feature.player.common.utils.PLAYER_SERVICE_LOG_TAG

/**
 * Останавливать сервис снаружи (Context.stopService) нельзя: pause/clearMediaItems едут по IPC
 * асинхронно и могут прийти уже после сноса сервиса. Поэтому UI присылает команду, а решение
 * о завершении принимает сам сервис — после выхода из foreground-состояния.
 */
@OptIn(UnstableApi::class)
internal class PlayerSessionCallback(
    private val stopState: PlayerServiceStopState,
    private val analyticsTracker: AnalyticsTracker,
    private val onStopRequested: () -> Unit,
) : MediaSession.Callback {
    override fun onConnect(
        session: MediaSession,
        controller: MediaSession.ControllerInfo,
    ): MediaSession.ConnectionResult {
        // Новый экран плеера успел подключиться к ещё не снесённому экземпляру: он снова
        // нужен, и запрет на foreground после остановки снимается.
        if (stopState.isStopping && !session.isMediaNotificationController(controller)) {
            analyticsTracker.log(PLAYER_SERVICE_LOG_TAG) { "Service reused after stop request" }
            stopState.isStopping = false
        }
        return MediaSession.ConnectionResult.AcceptedResultBuilder(session)
            .setAvailableSessionCommands(
                MediaSession.ConnectionResult.DEFAULT_SESSION_COMMANDS.buildUpon()
                    .add(PlayerSessionCommands.STOP_SERVICE)
                    .build(),
            )
            .build()
    }

    override fun onCustomCommand(
        session: MediaSession,
        controller: MediaSession.ControllerInfo,
        customCommand: SessionCommand,
        args: Bundle,
    ): ListenableFuture<SessionResult> {
        if (customCommand.customAction != PlayerSessionCommands.ACTION_STOP_SERVICE) {
            return super.onCustomCommand(session, controller, customCommand, args)
        }
        onStopRequested()
        return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
    }
}
