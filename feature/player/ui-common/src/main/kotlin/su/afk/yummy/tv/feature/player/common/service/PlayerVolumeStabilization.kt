package su.afk.yummy.tv.feature.player.common.service

import androidx.media3.common.C
import androidx.media3.common.Player
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import su.afk.yummy.tv.core.analytics.api.AnalyticsTracker
import su.afk.yummy.tv.feature.player.common.PlayerLoudnessNormalizer

/**
 * «Стабилизация громкости»: эффект пересобирается при смене либо настройки, либо аудио-сессии
 * (новая серия/переподключение плеера пересоздают session id).
 */
internal class PlayerVolumeStabilization(
    analyticsTracker: AnalyticsTracker,
) : Player.Listener {
    private val loudnessNormalizer = PlayerLoudnessNormalizer(analyticsTracker)
    private var enabled = false
    private var audioSessionId = C.AUDIO_SESSION_ID_UNSET

    /**
     * @param initialAudioSessionId начальная сессия: onAudioSessionIdChanged приходит не всегда
     *   до старта, поэтому текущее значение подхватывается сразу.
     */
    fun start(scope: CoroutineScope, enabledFlow: Flow<Boolean>, initialAudioSessionId: Int) {
        audioSessionId = initialAudioSessionId
        enabledFlow
            .onEach { value ->
                enabled = value
                loudnessNormalizer.apply(audioSessionId, value)
            }
            .launchIn(scope)
    }

    override fun onAudioSessionIdChanged(audioSessionId: Int) {
        this.audioSessionId = audioSessionId
        loudnessNormalizer.apply(audioSessionId, enabled)
    }

    fun release() {
        loudnessNormalizer.release()
    }
}
