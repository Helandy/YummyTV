package su.afk.yummy.tv.feature.player.common.service

import android.app.ActivityManager
import android.app.PendingIntent
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.cast.CastPlayer
import androidx.media3.cast.RemoteCastPlayer
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MimeTypes
import androidx.media3.common.Player
import androidx.media3.common.TrackGroup
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import su.afk.yummy.tv.core.analytics.api.AnalyticsTracker
import su.afk.yummy.tv.core.model.settings.PlayerBufferProfile
import su.afk.yummy.tv.core.preferences.settings.PlayerSettingsStore
import su.afk.yummy.tv.domain.player.session.AllohaPlaybackSessionManager
import su.afk.yummy.tv.feature.player.common.PlayerLoadControlFactory
import su.afk.yummy.tv.feature.player.common.PlayerLoudnessNormalizer
import javax.inject.Inject

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

    private var mediaSession: MediaSession? = null
    private var player: ExoPlayer? = null
    private var castPlayer: CastPlayer? = null

    // На TV кастуем некуда - Cast нужен только на мобилке (см. DeviceAwareTvIntegration
    // за тем же паттерном рантайм-детекта TV, поскольку :app - один манифест/APK на обе платформы).
    private val isTelevision: Boolean
        get() = resources.configuration.uiMode and Configuration.UI_MODE_TYPE_MASK ==
            Configuration.UI_MODE_TYPE_TELEVISION

    private val loudnessNormalizer = PlayerLoudnessNormalizer()
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    // Источник истины для «стабилизации громкости»: эффект пересобирается при смене либо
    // настройки, либо аудио-сессии (новая серия/переподключение плеера пересоздают session id).
    private var stabilizationEnabled = false
    private var currentAudioSessionId = C.AUDIO_SESSION_ID_UNSET

    override fun onCreate() {
        super.onCreate()
        // Фонового воспроизведения без экрана нет, поэтому «user engaged» окно не нужно: с
        // дефолтными 10 минутами media3 ещё долго после паузы считает playback ongoing и держит
        // сервис foreground-нужным, а любое завершение сервиса в этом окне система расценивает
        // как startForegroundService() без startForeground() и убивает процесс.
        setForegroundServiceTimeoutMs(0)
        val isLowRamDevice = isLowRamDevice()
        val trackSelector = DefaultTrackSelector(this).apply {
            // На слабых устройствах отдаём выбор битрейта адаптивному алгоритму вместо
            // принудительного максимума: меньше нагрузка на декодер и на буфер по памяти.
            setParameters(
                buildUponParameters().setForceHighestSupportedBitrate(!isLowRamDevice),
            )
        }
        // enableDecoderFallback: если аппаратный AVC-декодер не может инициализироваться
        // (например NO_MEMORY при config/start на некоторых устройствах/прошивках), без этого
        // флага Media3 просто кидает ошибку вместо попытки со следующим декодером в списке -
        // а для Alloha это уводит в бесконечный fresh-session recovery loop, каждый раз
        // упирающийся в тот же самый сломанный железный декодер.
        val renderersFactory = DefaultRenderersFactory(this)
            .setEnableDecoderFallback(true)
        val exoPlayer = ExoPlayer.Builder(this, renderersFactory)
            .setTrackSelector(trackSelector)
            .setMediaSourceFactory(
                DefaultMediaSourceFactory(playbackConfig.dataSourceFactory())
                    .setLoadErrorHandlingPolicy(PlayerLoadErrorHandlingPolicy(playbackConfig)),
            )
            .setLoadControl(PlayerLoadControlFactory.create(readBufferProfile()))
            // Фокус нужен, чтобы чужая музыка вставала на паузу при старте серии, а звонок или
            // навигатор ставили на паузу/приглушали нас. MOVIE, а не SPEECH: при duck-потере
            // ExoPlayer приглушает звук вместо паузы.
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                    .build(),
                /* handleAudioFocus = */
                true,
            )
            .setHandleAudioBecomingNoisy(true)
            .build()
        exoPlayer.addAnalyticsListener(PlayerDecoderAnalyticsListener(analyticsTracker))
        exoPlayer.addListener(object : Player.Listener {
            private var overriddenAudioGroup: TrackGroup? = null

            override fun onAudioSessionIdChanged(audioSessionId: Int) {
                currentAudioSessionId = audioSessionId
                loudnessNormalizer.apply(audioSessionId, stabilizationEnabled)
            }

            override fun onTracksChanged(tracks: Tracks) {
                val selection = playbackConfig.trackSelectionConfig()
                val audioGroups = tracks.groups.filter { it.type == C.TRACK_TYPE_AUDIO }
                if (selection.audioTrackPolicy != PlayerAudioTrackPolicy.FirstAudioGroup ||
                    audioGroups.isEmpty()
                ) {
                    clearAllohaAudioOverride(trackSelector)
                    return
                }
                val firstAudioGroup = audioGroups.first().mediaTrackGroup
                if (overriddenAudioGroup == firstAudioGroup) return
                overriddenAudioGroup = firstAudioGroup
                trackSelector.setParameters(
                    trackSelector.buildUponParameters()
                        .setPreferredAudioLanguage(ALLOHA_AUDIO_LANGUAGE)
                        .setPreferredVideoMimeType(MimeTypes.VIDEO_H264)
                        .setRendererDisabled(AUDIO_RENDERER_INDEX, false)
                        .setOverrideForType(TrackSelectionOverride(firstAudioGroup, 0))
                        .build(),
                )
                Log.i(
                    LOG_TAG,
                    "Alloha audio selected groups=${audioGroups.size} " +
                        "tracksInFirstGroup=${firstAudioGroup.length} group=0 track=0 " +
                        "offline=${selection.isOfflinePlayback}",
                )
            }

            private fun clearAllohaAudioOverride(trackSelector: DefaultTrackSelector) {
                if (overriddenAudioGroup == null) return
                overriddenAudioGroup = null
                trackSelector.setParameters(
                    trackSelector.buildUponParameters()
                        .setPreferredAudioLanguage(null)
                        .setPreferredTextLanguage(null)
                        .setPreferredVideoMimeType(null)
                        .clearOverridesOfType(C.TRACK_TYPE_AUDIO)
                        .build(),
                )
                Log.i(LOG_TAG, "Alloha audio override cleared")
            }
        })
        player = exoPlayer
        // Начальная сессия: onAudioSessionIdChanged приходит не всегда до старта, поэтому
        // подхватываем текущее значение сразу.
        currentAudioSessionId = exoPlayer.audioSessionId
        settingsStore.volumeStabilizationEnabled
            .onEach { enabled ->
                stabilizationEnabled = enabled
                loudnessNormalizer.apply(currentAudioSessionId, enabled)
            }
            .launchIn(serviceScope)
        if (!isTelevision) {
            castPlayer = buildCastPlayer(exoPlayer)
        }
        mediaSession = MediaSession.Builder(this, castPlayer ?: exoPlayer)
            .setSessionActivity(createSessionActivityPendingIntent())
            .setCallback(PlayerSessionCallback())
            .build()
    }

    /**
     * CastContext доступен только при наличии Google Play Services на устройстве - без них
     * CastContext.getSharedInstance() кидает исключение, поэтому локальный ExoPlayer остаётся
     * фолбэком, а не жёстким требованием.
     */
    private fun buildCastPlayer(exoPlayer: ExoPlayer): CastPlayer? =
        try {
            CastPlayer.Builder(this)
                .setLocalPlayer(exoPlayer)
                .setRemotePlayer(
                    RemoteCastPlayer.Builder(this)
                        .setMediaItemConverter(YummyTvCastMediaItemConverter())
                        .build(),
                )
                .build()
        } catch (e: Exception) {
            Log.w(LOG_TAG, "CastPlayer unavailable", e)
            null
        }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? =
        mediaSession

    override fun onTaskRemoved(rootIntent: Intent?) {
        // Видео-плеер не поддерживает воспроизведение звука в фоне без экрана: если пользователь
        // смахнул приложение из Recents, держать ExoPlayer (буфер + стриминг-кэш) в памяти незачем.
        // Гасим только через pauseAllPlayersAndStopSelf(): голый stopSelf() при ongoing playback
        // роняет процесс системным RemoteServiceException.
        pauseAllPlayersAndStopSelf()
    }

    /**
     * Останавливать сервис снаружи (Context.stopService) нельзя: pause/clearMediaItems едут по IPC
     * асинхронно и могут прийти уже после сноса сервиса. Поэтому UI присылает команду, а решение
     * о завершении принимает сам сервис — после выхода из foreground-состояния.
     */
    private inner class PlayerSessionCallback : MediaSession.Callback {
        override fun onConnect(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
        ): MediaSession.ConnectionResult =
            MediaSession.ConnectionResult.AcceptedResultBuilder(session)
                .setAvailableSessionCommands(
                    MediaSession.ConnectionResult.DEFAULT_SESSION_COMMANDS.buildUpon()
                        .add(PlayerSessionCommands.STOP_SERVICE)
                        .build(),
                )
                .build()

        override fun onCustomCommand(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            customCommand: SessionCommand,
            args: Bundle,
        ): ListenableFuture<SessionResult> {
            if (customCommand.customAction != PlayerSessionCommands.ACTION_STOP_SERVICE) {
                return super.onCustomCommand(session, controller, customCommand, args)
            }
            pauseAllPlayersAndStopSelf()
            return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
        }
    }

    override fun onDestroy() {
        allohaSessionManager.closeActive()
        serviceScope.cancel()
        loudnessNormalizer.release()
        // mediaSession.player - это castPlayer, если он собрался, а CastPlayer.release()
        // сам освобождает и обёрнутый localPlayer (exoPlayer) - отдельный exoPlayer.release() не нужен.
        mediaSession?.run {
            player.release()
            release()
        }
        mediaSession = null
        player = null
        castPlayer = null
        super.onDestroy()
    }

    /**
     * ExoPlayer.Builder требует LoadControl синхронно, поэтому профиль читается блокирующе — это
     * не забытый Dispatchers.IO. Таймаут страхует от подвисшего первого чтения DataStore: в этом
     * случае берётся то же значение по умолчанию, что и в настройках.
     */
    private fun readBufferProfile(): PlayerBufferProfile =
        runBlocking {
            withTimeoutOrNull(BUFFER_PROFILE_READ_TIMEOUT_MS) {
                settingsStore.playerBufferProfile.first()
            }
        } ?: PlayerBufferProfile.SMALL

    private fun isLowRamDevice(): Boolean =
        (getSystemService(ACTIVITY_SERVICE) as? ActivityManager)?.isLowRamDevice == true

    private fun createSessionActivityPendingIntent(): PendingIntent {
        val intent = packageManager.getLaunchIntentForPackage(packageName)
            ?: Intent(Intent.ACTION_MAIN).setPackage(packageName)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        return PendingIntent.getActivity(
            this,
            REQUEST_CODE_SESSION_ACTIVITY,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private companion object {
        const val LOG_TAG = "PlayerMediaSession"
        const val ALLOHA_AUDIO_LANGUAGE = "ru"
        const val AUDIO_RENDERER_INDEX = 1
        const val REQUEST_CODE_SESSION_ACTIVITY = 40_101
        const val BUFFER_PROFILE_READ_TIMEOUT_MS = 500L
    }
}
