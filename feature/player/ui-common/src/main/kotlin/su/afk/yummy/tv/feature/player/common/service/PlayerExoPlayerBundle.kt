package su.afk.yummy.tv.feature.player.common.service

import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector

/**
 * Собранный ExoPlayer и его трек-селектор: селектор нужен слушателям для подмены дорожек.
 * [videoEffectsReady] — конвейер видеоэффектов поднят при сборке (Deblur был включён), эффекты можно
 * менять на лету без перезапуска потока.
 */
internal data class PlayerExoPlayerBundle(
    val exoPlayer: ExoPlayer,
    val trackSelector: DefaultTrackSelector,
    val videoEffectsReady: Boolean,
)
