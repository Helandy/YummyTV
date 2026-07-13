package su.afk.yummy.tv.feature.player.common.service

import androidx.media3.common.Player
import su.afk.yummy.tv.feature.player.common.PlayerMediaItemFactory

enum class PlayerAudioTrackPolicy {
    Default,
    FirstAudioGroup,
}

data class PlayerMediaItemConfig(
    val playbackKey: String,
    val mediaItemKey: String,
    val url: String,
    val title: String,
    val artist: String,
    val subtitle: String?,
    val description: String?,
    val artworkUrl: String?,
    val durationMs: Long,
    val headers: Map<String, String>,
    val offlineCacheKey: String?,
    val isOfflinePlayback: Boolean,
    val useRotatingHlsCacheKeys: Boolean,
    val audioTrackPolicy: PlayerAudioTrackPolicy,
    val playbackPositionMs: Long,
    val resumeFromMs: Long,
)

class PlayerMediaItemUpdater {
    private var playbackKey: String? = null
    private var mediaItemKey: String? = null

    fun update(
        player: Player,
        playbackConfig: PlayerPlaybackConfig,
        config: PlayerMediaItemConfig
    ) {
        val item = PlayerMediaItemFactory.mediaItemFor(
            url = config.url,
            title = config.title,
            artist = config.artist,
            subtitle = config.subtitle,
            description = config.description,
            artworkUri = config.artworkUrl,
            durationMs = config.durationMs,
            customCacheKey = config.offlineCacheKey,
        )
        playbackConfig.updateStream(
            headers = config.headers,
            offlineCacheKey = config.offlineCacheKey.takeIf { config.isOfflinePlayback },
            offlineManifestUri = config.url.takeIf { config.isOfflinePlayback },
            useRotatingHlsCacheKeys = config.useRotatingHlsCacheKeys,
            audioTrackPolicy = config.audioTrackPolicy,
            isOfflinePlayback = config.isOfflinePlayback,
        )
        val resume = config.playbackPositionMs.takeIf { it > 0L } ?: config.resumeFromMs
        if (player.currentMediaItem?.localConfiguration?.uri?.toString() != config.url || playbackKey != config.playbackKey) {
            player.setMediaItem(item, resume)
            player.prepare()
            playbackKey = config.playbackKey
            mediaItemKey = config.mediaItemKey
        } else if (mediaItemKey != config.mediaItemKey && player.mediaItemCount > 0) {
            val index = player.currentMediaItemIndex
            if (index >= 0) player.replaceMediaItem(index, item) else {
                player.setMediaItem(item, resume)
                player.prepare()
            }
            mediaItemKey = config.mediaItemKey
        }
    }
}
