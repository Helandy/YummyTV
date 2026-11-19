package su.afk.yummy.tv.feature.player.common.service

import androidx.media3.common.Player
import su.afk.yummy.tv.domain.player.utils.maxVideoHeightOrNull
import su.afk.yummy.tv.feature.player.common.PlayerMediaItemFactory
import su.afk.yummy.tv.feature.player.utils.isSyntheticMasterUrl

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
    /** `VideoDownloadCacheKeyScheme.storageValue` записи загрузки; 0 для онлайн-воспроизведения. */
    val offlineCacheKeyScheme: Int,
    val isOfflinePlayback: Boolean,
    val isLocalFile: Boolean,
    val useRotatingHlsCacheKeys: Boolean,
    val audioTrackPolicy: PlayerAudioTrackPolicy,
    val playbackPositionMs: Long,
    val resumeFromMs: Long,
    /** Side-loaded Alloha subtitle file, if the user picked one. */
    val subtitleUrl: String? = null,
    val subtitleMimeType: String? = null,
    val subtitleLanguage: String? = null,
    val subtitleLabel: String? = null,
    val silentReconnectEnabled: Boolean = false,
    /** Потолок высоты для синтетического master (адаптивный режим); `null` — берётся из ссылки. */
    val videoHeightCap: Int? = null,
    /** Ручной выбор качества в синтетическом master: играть именно этот вариант. */
    val videoHeightPinned: Boolean = false,
)

class PlayerMediaItemUpdater {
    private var playbackKey: String? = null
    private var mediaItemKey: String? = null
    private var defaultForceHighestBitrate: Boolean? = null

    fun update(
        player: Player,
        playbackConfig: PlayerPlaybackConfig,
        config: PlayerMediaItemConfig
    ) {
        val item = PlayerMediaItemFactory.mediaItemFor(
            url = config.url,
            mediaId = config.playbackKey,
            title = config.title,
            artist = config.artist,
            subtitle = config.subtitle,
            description = config.description,
            artworkUri = config.artworkUrl,
            durationMs = config.durationMs,
            customCacheKey = config.offlineCacheKey,
            subtitleUrl = config.subtitleUrl,
            subtitleMimeType = config.subtitleMimeType,
            subtitleLanguage = config.subtitleLanguage,
            subtitleLabel = config.subtitleLabel,
        )
        playbackConfig.updateStream(
            headers = config.headers,
            offlineCacheKey = config.offlineCacheKey.takeIf { config.isOfflinePlayback },
            offlineManifestUri = config.url.takeIf { config.isOfflinePlayback },
            offlineCacheKeyScheme = config.offlineCacheKeyScheme,
            useRotatingHlsCacheKeys = config.useRotatingHlsCacheKeys,
            audioTrackPolicy = config.audioTrackPolicy,
            isOfflinePlayback = config.isOfflinePlayback,
            isLocalFile = config.isLocalFile,
            silentReconnectEnabled = config.silentReconnectEnabled,
        )
        player.applyVideoHeightCap(config.videoHeightCap ?: config.url.maxVideoHeightOrNull(), config.videoHeightPinned)
        applyAdaptiveBitrate(player, adaptive = config.url.isSyntheticMasterUrl())
        val resume = config.playbackPositionMs.takeIf { it > 0L } ?: config.resumeFromMs
        val currentUri = player.currentMediaItem?.localConfiguration?.uri?.toString()
        if (currentUri != config.url || playbackKey != config.playbackKey) {
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

    /**
     * Синтетический master отдаёт выбор варианта ExoPlayer: принудительный максимум битрейта (по умолчанию
     * на обычных устройствах) выключаем. Для остальных потоков возвращаем исходное значение плеера.
     */
    private fun applyAdaptiveBitrate(player: Player, adaptive: Boolean) {
        val current = player.trackSelectionParameters
        val default = defaultForceHighestBitrate
            ?: current.forceHighestSupportedBitrate.also { defaultForceHighestBitrate = it }
        val target = if (adaptive) false else default
        if (current.forceHighestSupportedBitrate == target) return
        player.trackSelectionParameters = current.buildUpon().setForceHighestSupportedBitrate(target).build()
    }
}

/** Потолок высоты из ссылки (`withMaxVideoHeight`); без него возвращает лимит по умолчанию, чтобы он не перешёл на следующий поток. */
private fun Player.applyVideoHeightCap(height: Int?, pinned: Boolean = false) {
    val current = trackSelectionParameters
    val maxHeight = height ?: Int.MAX_VALUE
    val minHeight = if (pinned && height != null) height else 0
    if (current.maxVideoHeight == maxHeight && current.minVideoHeight == minHeight) return
    trackSelectionParameters = current.buildUpon()
        .setMaxVideoSize(Int.MAX_VALUE, maxHeight)
        .setMinVideoSize(0, minHeight)
        .build()
}
