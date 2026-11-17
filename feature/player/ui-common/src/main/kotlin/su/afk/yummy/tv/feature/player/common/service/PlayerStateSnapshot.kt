package su.afk.yummy.tv.feature.player.common.service

import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionParameters

/**
 * Состояние воспроизведения, которое переносится на пересобранный плеер: очередь, позиция, пауза,
 * скорость, громкость и выбор дорожек (качество, озвучка, субтитры).
 */
internal data class PlayerStateSnapshot(
    val mediaItems: List<MediaItem>,
    val mediaItemIndex: Int,
    val positionMs: Long,
    val playWhenReady: Boolean,
    val prepared: Boolean,
    val playbackParameters: PlaybackParameters,
    val repeatMode: Int,
    val shuffleModeEnabled: Boolean,
    val volume: Float,
    val trackSelectionParameters: TrackSelectionParameters,
) {
    fun restoreTo(player: Player) {
        player.trackSelectionParameters = trackSelectionParameters
        player.playbackParameters = playbackParameters
        player.repeatMode = repeatMode
        player.shuffleModeEnabled = shuffleModeEnabled
        player.volume = volume
        if (mediaItems.isNotEmpty()) {
            player.setMediaItems(mediaItems, mediaItemIndex, positionMs)
            if (prepared) player.prepare()
        }
        player.playWhenReady = playWhenReady
    }

    companion object {
        fun capture(player: Player): PlayerStateSnapshot = PlayerStateSnapshot(
            mediaItems = List(player.mediaItemCount, player::getMediaItemAt),
            mediaItemIndex = player.currentMediaItemIndex,
            positionMs = player.currentPosition,
            playWhenReady = player.playWhenReady,
            prepared = player.playbackState != Player.STATE_IDLE,
            playbackParameters = player.playbackParameters,
            repeatMode = player.repeatMode,
            shuffleModeEnabled = player.shuffleModeEnabled,
            volume = player.volume,
            trackSelectionParameters = player.trackSelectionParameters,
        )
    }
}
