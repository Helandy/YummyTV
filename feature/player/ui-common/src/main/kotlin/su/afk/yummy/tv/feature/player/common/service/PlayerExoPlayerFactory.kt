package su.afk.yummy.tv.feature.player.common.service

import android.app.ActivityManager
import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import su.afk.yummy.tv.core.analytics.api.AnalyticsTracker
import su.afk.yummy.tv.core.model.settings.PlayerBufferProfile
import su.afk.yummy.tv.core.preferences.settings.PlayerSettingsStore
import su.afk.yummy.tv.feature.player.common.PlayerLoadControlFactory
import javax.inject.Inject

/** Собирает ExoPlayer сервиса: трек-селектор, декодеры, источники, буфер и аудио-фокус. */
@OptIn(UnstableApi::class)
internal class PlayerExoPlayerFactory @Inject constructor(
    private val playbackConfig: PlayerPlaybackConfig,
    private val settingsStore: PlayerSettingsStore,
    private val analyticsTracker: AnalyticsTracker,
) {
    fun create(context: Context): PlayerExoPlayerBundle {
        val isLowRamDevice = context.isLowRamDevice()
        val trackSelector = DefaultTrackSelector(context).apply {
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
        val renderersFactory = DefaultRenderersFactory(context)
            .setEnableDecoderFallback(true)
        val bufferProfile = readBufferProfile()
        val exoPlayer = ExoPlayer.Builder(context, renderersFactory)
            .setTrackSelector(trackSelector)
            .setMediaSourceFactory(
                DefaultMediaSourceFactory(playbackConfig.dataSourceFactory())
                    .setLoadErrorHandlingPolicy(PlayerLoadErrorHandlingPolicy(playbackConfig)),
            )
            .setLoadControl(PlayerLoadControlFactory.create(bufferProfile))
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
        exoPlayer.addAnalyticsListener(
            PlayerBufferingAnalyticsListener(
                tracker = analyticsTracker,
                profile = bufferProfile,
                currentUriHost = { exoPlayer.currentMediaItem?.localConfiguration?.uri?.host },
            ),
        )
        return PlayerExoPlayerBundle(exoPlayer, trackSelector)
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

    private fun Context.isLowRamDevice(): Boolean =
        (getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager)?.isLowRamDevice == true

    private companion object {
        const val BUFFER_PROFILE_READ_TIMEOUT_MS = 500L
    }
}
