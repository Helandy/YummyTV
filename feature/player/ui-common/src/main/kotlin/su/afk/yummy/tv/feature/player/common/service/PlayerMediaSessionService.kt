package su.afk.yummy.tv.feature.player.common.service

import android.content.Intent
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import su.afk.yummy.tv.core.analytics.api.AnalyticsTracker
import su.afk.yummy.tv.core.preferences.settings.PlayerSettingsStore
import su.afk.yummy.tv.domain.player.session.AllohaPlaybackSessionManager
import su.afk.yummy.tv.feature.player.common.utils.PLAYER_SERVICE_LOG_TAG
import su.afk.yummy.tv.feature.player.common.utils.createPlayerSessionActivityIntent
import javax.inject.Inject

/**
 * Сервис медиа-сессии плеера. Сам только связывает компоненты и отвечает за жизненный цикл и
 * остановку; сборка плеера, подмена дорожек Alloha, стабилизация громкости и Cast вынесены.
 */
@OptIn(UnstableApi::class)
@AndroidEntryPoint
class PlayerMediaSessionService : MediaSessionService() {
    @Inject
    internal lateinit var playbackConfig: PlayerPlaybackConfig

    @Inject
    internal lateinit var allohaSessionManager: AllohaPlaybackSessionManager

    @Inject
    internal lateinit var settingsStore: PlayerSettingsStore

    @Inject
    internal lateinit var analyticsTracker: AnalyticsTracker

    @Inject
    internal lateinit var exoPlayerFactory: PlayerExoPlayerFactory

    @Inject
    internal lateinit var castPlayerFactory: PlayerCastPlayerFactory

    private var mediaSession: MediaSession? = null
    private var volumeStabilization: PlayerVolumeStabilization? = null
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val stopState = PlayerServiceStopState()

    override fun onCreate() {
        super.onCreate()
        // Фонового воспроизведения без экрана нет, поэтому «user engaged» окно не нужно: с
        // дефолтными 10 минутами media3 ещё долго после паузы считает playback ongoing и держит
        // сервис foreground-нужным, а любое завершение сервиса в этом окне система расценивает
        // как startForegroundService() без startForeground() и убивает процесс.
        setForegroundServiceTimeoutMs(0)
        analyticsTracker.log(PLAYER_SERVICE_LOG_TAG) { "Service onCreate" }
        val (exoPlayer, trackSelector) = exoPlayerFactory.create(this)
        val stabilization = PlayerVolumeStabilization(analyticsTracker)
        exoPlayer.addListener(stabilization)
        exoPlayer.addListener(
            PlayerAllohaAudioOverride(trackSelector, playbackConfig, analyticsTracker),
        )
        stabilization.start(
            scope = serviceScope,
            enabledFlow = settingsStore.volumeStabilizationEnabled,
            initialAudioSessionId = exoPlayer.audioSessionId,
        )
        volumeStabilization = stabilization
        val castPlayer = castPlayerFactory.createOrNull(this, exoPlayer)
        mediaSession = MediaSession.Builder(this, castPlayer ?: exoPlayer)
            .setSessionActivity(createPlayerSessionActivityIntent())
            .setCallback(
                PlayerSessionCallback(
                    stopState = stopState,
                    analyticsTracker = analyticsTracker,
                    onStopRequested = { stopService(reason = "command") },
                ),
            )
            .build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? =
        mediaSession

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        analyticsTracker.log(PLAYER_SERVICE_LOG_TAG) {
            "Service onStartCommand startId=$startId action=${intent?.action} " +
                "stopping=${stopState.isStopping}"
        }
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        // Видео-плеер не поддерживает воспроизведение звука в фоне без экрана: если пользователь
        // смахнул приложение из Recents, держать ExoPlayer (буфер + стриминг-кэш) в памяти незачем.
        stopService(reason = "taskRemoved")
    }

    /**
     * Внутренний контроллер уведомлений media3 узнаёт о паузе с задержкой: отложенное обновление
     * после stopSelf() ещё видит playWhenReady=true и снова зовёт startForegroundService(). Если
     * запись сервиса к этому моменту уже снесена, система поднимает новый экземпляр с флагом
     * «обязан выйти в foreground», а startForeground() старого экземпляра игнорирует — процесс
     * падает с RemoteServiceException. Поэтому после начала остановки уведомление не обновляем.
     */
    override fun onUpdateNotification(session: MediaSession, startInForegroundRequired: Boolean) {
        if (stopState.isStopping) {
            analyticsTracker.log(PLAYER_SERVICE_LOG_TAG) {
                "Notification update skipped while stopping foreground=$startInForegroundRequired"
            }
            return
        }
        super.onUpdateNotification(session, startInForegroundRequired)
    }

    /**
     * Гасим только через pauseAllPlayersAndStopSelf(): голый stopSelf() при ongoing playback
     * роняет процесс системным RemoteServiceException.
     */
    private fun stopService(reason: String) {
        analyticsTracker.log(PLAYER_SERVICE_LOG_TAG) {
            "Service stop reason=$reason ongoing=$isPlaybackOngoing stopping=${stopState.isStopping}"
        }
        stopState.isStopping = true
        pauseAllPlayersAndStopSelf()
    }

    override fun onDestroy() {
        analyticsTracker.log(PLAYER_SERVICE_LOG_TAG) {
            "Service onDestroy stopping=${stopState.isStopping}"
        }
        allohaSessionManager.closeActive()
        serviceScope.cancel()
        volumeStabilization?.release()
        volumeStabilization = null
        // mediaSession.player - это castPlayer, если он собрался, а CastPlayer.release()
        // сам освобождает и обёрнутый localPlayer (exoPlayer) - отдельный exoPlayer.release() не нужен.
        mediaSession?.run {
            player.release()
            release()
        }
        mediaSession = null
        super.onDestroy()
    }
}
