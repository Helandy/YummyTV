package su.afk.yummy.tv.feature.player.common

import android.media.audiofx.DynamicsProcessing
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.media3.common.C
import su.afk.yummy.tv.core.analytics.api.AnalyticsTracker

/**
 * «Стабилизация громкости» — сжатие динамического диапазона звука через системный аудио-эффект
 * [DynamicsProcessing]: тихие диалоги подтягиваются вверх (make-up gain компрессора), громкие
 * сцены прижимаются (компрессор + лимитер против клиппинга). Эффект навешивается на
 * `audioSessionId` ExoPlayer, поэтому живёт только внутри плеер-сервиса.
 *
 * [DynamicsProcessing] доступен с Android 9 (API 28). На более старых версиях (проект собирается
 * с `minSdk = 24`) класс превращается в no-op — см. [isSupported]. Устройства без реализации
 * эффекта тоже деградируют тихо (см. try/catch в [attach]).
 *
 * Не потокобезопасен: все вызовы ожидаются с потока плеер-сервиса (main).
 */
class PlayerLoudnessNormalizer(
    private val analyticsTracker: AnalyticsTracker,
) {

    private var effect: DynamicsProcessing? = null
    private var attachedSessionId: Int = C.AUDIO_SESSION_ID_UNSET

    /** Поддерживает ли текущая версия Android сжатие динамического диапазона. */
    val isSupported: Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.P

    /**
     * Приводит эффект к желаемому состоянию для сессии [audioSessionId]. Идемпотентно: повторный
     * вызов с той же сессией и тем же [enabled] ничего не пересоздаёт.
     */
    fun apply(audioSessionId: Int, enabled: Boolean) {
        if (!isSupported || !enabled || audioSessionId == C.AUDIO_SESSION_ID_UNSET) {
            release()
            return
        }
        if (effect != null && attachedSessionId == audioSessionId) return
        release()
        attach(audioSessionId)
    }

    @RequiresApi(Build.VERSION_CODES.P)
    private fun attach(audioSessionId: Int) {
        try {
            val mbc =
                DynamicsProcessing.Mbc(/* inUse = */ true, /* enabled = */ true, BAND_COUNT).apply {
                    setBand(
                        0,
                        DynamicsProcessing.MbcBand(
                            /* enabled = */ true,
                            /* cutoffFrequency = */ BAND_CUTOFF_HZ,
                            /* attackTime = */ COMP_ATTACK_MS,
                            /* releaseTime = */ COMP_RELEASE_MS,
                            /* ratio = */ COMP_RATIO,
                            /* threshold = */ COMP_THRESHOLD_DB,
                            /* kneeWidth = */ COMP_KNEE_DB,
                            /* noiseGateThreshold = */ NOISE_GATE_DB,
                            /* expanderRatio = */ NO_EXPANSION_RATIO,
                            /* preGain = */ 0f,
                            /* postGain = */ COMP_MAKEUP_GAIN_DB,
                        ),
                    )
                }
            val limiter = DynamicsProcessing.Limiter(
                /* inUse = */ true,
                /* enabled = */ true,
                /* linkGroup = */ 0,
                /* attackTime = */ LIMITER_ATTACK_MS,
                /* releaseTime = */ LIMITER_RELEASE_MS,
                /* ratio = */ LIMITER_RATIO,
                /* threshold = */ LIMITER_THRESHOLD_DB,
                /* postGain = */ 0f,
            )
            val config = DynamicsProcessing.Config.Builder(
                DynamicsProcessing.VARIANT_FAVOR_FREQUENCY_RESOLUTION,
                CHANNEL_COUNT,
                /* preEqInUse = */ false,
                /* preEqBandCount = */ 0,
                /* mbcInUse = */ true,
                /* mbcBandCount = */ BAND_COUNT,
                /* postEqInUse = */ false,
                /* postEqBandCount = */ 0,
                /* limiterInUse = */ true,
            )
                .setMbcAllChannelsTo(mbc)
                .setLimiterAllChannelsTo(limiter)
                .build()

            val processing = DynamicsProcessing(EFFECT_PRIORITY, audioSessionId, config)
            processing.setEnabled(true)
            effect = processing
            attachedSessionId = audioSessionId
            analyticsTracker.log(LOG_TAG) { "Volume stabilization attached to session=$audioSessionId" }
        } catch (e: RuntimeException) {
            // Часть устройств не реализует DynamicsProcessing (нет системного эффекта) — тихо
            // деградируем в no-op, не роняя воспроизведение.
            analyticsTracker.log(LOG_TAG, e) { "Volume stabilization unavailable on this device" }
            releaseEffect()
        }
    }

    /** Освобождает эффект (если был). Безопасно вызывать многократно и без предварительного [apply]. */
    fun release() {
        releaseEffect()
    }

    private fun releaseEffect() {
        effect?.let { runCatching { it.release() } }
        effect = null
        attachedSessionId = C.AUDIO_SESSION_ID_UNSET
    }

    private companion object {
        const val LOG_TAG = "PlayerLoudness"
        const val EFFECT_PRIORITY = 0
        const val CHANNEL_COUNT = 2
        const val BAND_COUNT = 1
        const val BAND_CUTOFF_HZ = 20_000f

        // Компрессор: подтягиваем тихое (make-up), прижимаем громкое выше порога.
        const val COMP_THRESHOLD_DB = -24f
        const val COMP_RATIO = 4f
        const val COMP_ATTACK_MS = 10f
        const val COMP_RELEASE_MS = 150f
        const val COMP_KNEE_DB = 6f
        const val COMP_MAKEUP_GAIN_DB = 6f
        const val NOISE_GATE_DB = -80f
        const val NO_EXPANSION_RATIO = 1f

        // Лимитер-«кирпич» против клиппинга после make-up gain.
        const val LIMITER_THRESHOLD_DB = -2f
        const val LIMITER_RATIO = 10f
        const val LIMITER_ATTACK_MS = 1f
        const val LIMITER_RELEASE_MS = 60f
    }
}
