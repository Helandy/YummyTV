package su.afk.yummy.tv.feature.player.common

import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.LoadControl
import su.afk.yummy.tv.core.model.settings.PlayerBufferProfile

@UnstableApi
object PlayerLoadControlFactory {
    // Настраивается только оперативный буфер ExoPlayer: это не дисковый кэш и не офлайн-загрузка.
    // Профиль выбирает пользователь в настройках; чем больше запас, тем больше памяти забирает
    // плеер — на слабых приставках это заметно. Один LoadControl на все источники; чем отличаются
    // Kodik/CVH и Alloha вокруг него — в docs/player-buffering.md.
    fun create(profile: PlayerBufferProfile): LoadControl =
        DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                profile.minBufferMs,
                profile.maxBufferMs,
                profile.bufferForPlaybackMs,
                profile.bufferForPlaybackAfterRebufferMs,
            )
            .setTargetBufferBytes(profile.targetBufferBytes)
            .setPrioritizeTimeOverSizeThresholds(PRIORITIZE_TIME_OVER_SIZE_THRESHOLDS)
            .build()

    // Не заставляем плеер любой ценой добирать время буфера, если уже достигнут лимит по памяти.
    private const val PRIORITIZE_TIME_OVER_SIZE_THRESHOLDS = false
}
