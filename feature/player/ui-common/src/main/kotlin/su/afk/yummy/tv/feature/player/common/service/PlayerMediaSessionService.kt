package su.afk.yummy.tv.feature.player.common.service

import android.content.Intent
import androidx.annotation.OptIn
import androidx.media3.cast.CastPlayer
import androidx.media3.common.DeviceInfo
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import su.afk.yummy.tv.core.analytics.api.AnalyticsTracker
import su.afk.yummy.tv.core.model.settings.VideoEnhancementMode
import su.afk.yummy.tv.core.preferences.settings.PlayerSettingsStore
import su.afk.yummy.tv.feature.player.common.utils.PLAYER_SERVICE_LOG_TAG
import su.afk.yummy.tv.feature.player.common.utils.createPlayerSessionActivityIntent
import su.afk.yummy.tv.feature.player.common.utils.isOutputSurfaceChangeFailure
import su.afk.yummy.tv.feature.player.common.utils.videoEffects
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
    internal lateinit var settingsStore: PlayerSettingsStore

    @Inject
    internal lateinit var analyticsTracker: AnalyticsTracker

    @Inject
    internal lateinit var exoPlayerFactory: PlayerExoPlayerFactory

    @Inject
    internal lateinit var castPlayerFactory: PlayerCastPlayerFactory

    private var mediaSession: MediaSession? = null
    private var exoPlayer: ExoPlayer? = null
    private var castPlayer: CastPlayer? = null
    private var sessionPlayer: PlayerVideoOutputTracker? = null
    private var videoEnhancement: PlayerVideoEnhancement? = null
    private var volumeStabilization: PlayerVolumeStabilization? = null
    private var debugFrameReceiver: PlayerDebugFrameReceiver? = null
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
        val stabilization = PlayerVolumeStabilization(analyticsTracker)
        volumeStabilization = stabilization
        val bundle = exoPlayerFactory.create(this)
        val player = wrapForSession(bundle)
        stabilization.start(
            scope = serviceScope,
            enabledFlow = settingsStore.volumeStabilizationEnabled,
            initialAudioSessionId = bundle.exoPlayer.audioSessionId,
        )
        debugFrameReceiver = PlayerDebugFrameReceiver.registerIfDebuggable(this, { exoPlayer }, settingsStore)
        videoEnhancement = PlayerVideoEnhancement(
            player = bundle.exoPlayer,
            pipelineReady = bundle.videoEffectsReady,
            effectsFor = { mode -> mode.videoEffects() },
            rebuildPlayer = ::rebuildPlayerWithoutEffects,
        ).also { it.start(scope = serviceScope, modeFlow = settingsStore.videoEnhancementMode) }
        // Начальное значение уже учтено при сборке плеера; дальше смена настройки действует сразу.
        settingsStore.audioFocusDisabled
            .distinctUntilChanged()
            .drop(1)
            .onEach { disabled ->
                exoPlayer?.setAudioAttributes(
                    PlayerExoPlayerFactory.MEDIA_AUDIO_ATTRIBUTES,
                    /* handleAudioFocus = */
                    !disabled,
                )
            }
            .launchIn(serviceScope)
        mediaSession = MediaSession.Builder(this, player)
            .setBitmapLoader(CopyingBitmapLoader(this))
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

    /** Подключает слушателей сервиса к собранному ExoPlayer и оборачивает его в плеер для сессии. */
    private fun wrapForSession(bundle: PlayerExoPlayerBundle): PlayerVideoOutputTracker {
        val player = bundle.exoPlayer
        volumeStabilization?.let(player::addListener)
        player.addListener(
            PlayerAllohaAudioOverride(bundle.trackSelector, playbackConfig, analyticsTracker),
        )
        player.addListener(
            object : Player.Listener {
                override fun onPlayerError(error: PlaybackException) {
                    if (error.errorCode in VIDEO_EFFECT_ERROR_CODES) onVideoEnhancementFailed(error)
                    if (error.isOutputSurfaceChangeFailure()) onOutputSurfaceChangeFailed(error)
                }
            },
        )
        val cast = castPlayerFactory.createOrNull(this, player)
        exoPlayer = player
        castPlayer = cast
        return PlayerVideoOutputTracker(cast ?: player).also { sessionPlayer = it }
    }

    /**
     * Эффект улучшения не собрался или упал на устройстве (например, GLES2 без FP16-текстур для Anime4K):
     * режим выключается в настройках, плеер пересобирается без эффектов и продолжает с той же позиции.
     */
    private fun onVideoEnhancementFailed(error: PlaybackException) {
        val mode = videoEnhancement?.mode ?: return
        if (mode == VideoEnhancementMode.OFF) return
        analyticsTracker.log(PLAYER_SERVICE_LOG_TAG, error) { "Video enhancement $mode failed, turning it off" }
        serviceScope.launch { settingsStore.setVideoEnhancementMode(VideoEnhancementMode.OFF) }
    }

    /**
     * Декодер устройства не принял смену выходной поверхности при смене потока (адаптивное качество меняет
     * поток на лету). Адаптивное качество выключается, чтобы ретрай плеера не повторял тот же сбой.
     */
    private fun onOutputSurfaceChangeFailed(error: PlaybackException) {
        analyticsTracker.log(PLAYER_SERVICE_LOG_TAG, error) {
            "Output surface change rejected by the decoder, turning adaptive quality off"
        }
        serviceScope.launch { settingsStore.setAdaptiveVideoQualityEnabled(false) }
    }

    /**
     * Убирает GL-конвейер улучшения видео без выхода из плеера: Media3 освобождает его только вместе с плеером, поэтому
     * ExoPlayer пересобирается без эффектов, получает очередь, позицию и выбор дорожек старого, а сессия
     * переключается на него — экран остаётся подключён к той же сессии. Во время трансляции на Cast локального
     * видео нет, и пересборка откладывается до следующего запуска сервиса.
     */
    private fun rebuildPlayerWithoutEffects() {
        val session = mediaSession ?: return
        val oldPlayer = exoPlayer ?: return
        val oldSessionPlayer = sessionPlayer ?: return
        if (castPlayer?.deviceInfo?.playbackType == DeviceInfo.PLAYBACK_TYPE_REMOTE) {
            analyticsTracker.log(PLAYER_SERVICE_LOG_TAG) { "Video enhancement off: rebuild skipped while casting" }
            return
        }
        val snapshot = PlayerStateSnapshot.capture(oldPlayer)
        val output = oldSessionPlayer.detachVideoOutput()
        // CastPlayer.release() освобождает и обёрнутый ExoPlayer.
        (castPlayer ?: oldPlayer).release()
        val bundle = exoPlayerFactory.create(this, enhancementMode = VideoEnhancementMode.OFF)
        val newSessionPlayer = wrapForSession(bundle)
        volumeStabilization?.onAudioSessionIdChanged(bundle.exoPlayer.audioSessionId)
        newSessionPlayer.attachVideoOutput(output)
        snapshot.restoreTo(bundle.exoPlayer)
        videoEnhancement?.attach(bundle.exoPlayer, pipelineReady = bundle.videoEffectsReady)
        session.setPlayer(newSessionPlayer)
        analyticsTracker.log(PLAYER_SERVICE_LOG_TAG) {
            "Video enhancement off: player rebuilt items=${snapshot.mediaItems.size} positionMs=${snapshot.positionMs}"
        }
    }

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
        serviceScope.cancel()
        debugFrameReceiver?.let { unregisterReceiver(it) }
        debugFrameReceiver = null
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

    private companion object {
        /** Ошибки GL-конвейера эффектов: инициализация (шейдеры, текстуры) и обработка кадра. */
        val VIDEO_EFFECT_ERROR_CODES = setOf(
            PlaybackException.ERROR_CODE_VIDEO_FRAME_PROCESSOR_INIT_FAILED,
            PlaybackException.ERROR_CODE_VIDEO_FRAME_PROCESSING_FAILED,
        )
    }
}
