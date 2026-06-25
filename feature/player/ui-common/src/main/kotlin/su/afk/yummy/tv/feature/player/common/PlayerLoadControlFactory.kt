package su.afk.yummy.tv.feature.player.common

import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.LoadControl

@UnstableApi
object PlayerLoadControlFactory {
    // Увеличиваем только оперативный буфер ExoPlayer: это не дисковый кэш и не офлайн-загрузка.
    fun create(): LoadControl =
        DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                MIN_BUFFER_MS,
                MAX_BUFFER_MS,
                BUFFER_FOR_PLAYBACK_MS,
                BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS,
            )
            .setTargetBufferBytes(TARGET_BUFFER_BYTES)
            .setPrioritizeTimeOverSizeThresholds(PRIORITIZE_TIME_OVER_SIZE_THRESHOLDS)
            .build()

    // Balanced-профиль: старт остаётся быстрым, а при воспроизведении плеер держит больший запас данных.
    private const val MIN_BUFFER_MS = 50_000
    private const val MAX_BUFFER_MS = 120_000
    private const val BUFFER_FOR_PLAYBACK_MS = 2_500
    private const val BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS = 5_000

    // Ограничиваем целевой размер буфера, чтобы слабые TV-устройства не забирали слишком много памяти.
    private const val TARGET_BUFFER_BYTES = 64 * 1024 * 1024

    // Не заставляем плеер любой ценой добирать время буфера, если уже достигнут лимит по памяти.
    private const val PRIORITIZE_TIME_OVER_SIZE_THRESHOLDS = false
}
