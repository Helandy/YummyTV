package su.afk.yummy.tv.feature.player.common.service

import androidx.media3.datasource.DataSource
import androidx.media3.datasource.cache.CacheDataSource
import su.afk.yummy.tv.feature.player.common.PlayerDataSourceFactory
import su.afk.yummy.tv.feature.videodownload.playback.VideoDownloadPlaybackCache
import javax.inject.Inject
import javax.inject.Singleton

interface PlayerPlaybackConfig {
    fun updateStream(
        headers: Map<String, String>,
        offlineCacheKey: String?,
        offlineManifestUri: String?,
        useRotatingHlsCacheKeys: Boolean,
        audioTrackPolicy: PlayerAudioTrackPolicy,
        isOfflinePlayback: Boolean,
    )

    fun dataSourceFactory(): DataSource.Factory
    fun trackSelectionConfig(): PlayerTrackSelectionConfig
}

data class PlayerTrackSelectionConfig(
    val audioTrackPolicy: PlayerAudioTrackPolicy = PlayerAudioTrackPolicy.Default,
    val isOfflinePlayback: Boolean = false,
)

private data class OfflineCacheConfig(
    val cacheKey: String,
    val manifestUri: String?,
    val useRotatingHlsCacheKeys: Boolean,
)

@Singleton
class DefaultPlayerPlaybackConfig @Inject constructor(
    private val downloadPlaybackCache: VideoDownloadPlaybackCache,
    private val streamingCacheProvider: PlayerStreamingCacheProvider,
) : PlayerPlaybackConfig {
    @Volatile
    private var headers: Map<String, String> = emptyMap()

    @Volatile
    private var offlineCacheConfig: OfflineCacheConfig? = null

    @Volatile
    private var trackSelection = PlayerTrackSelectionConfig()

    override fun updateStream(
        headers: Map<String, String>,
        offlineCacheKey: String?,
        offlineManifestUri: String?,
        useRotatingHlsCacheKeys: Boolean,
        audioTrackPolicy: PlayerAudioTrackPolicy,
        isOfflinePlayback: Boolean,
    ) {
        this.headers = headers.toMap()
        offlineCacheConfig = offlineCacheKey?.let { cacheKey ->
            OfflineCacheConfig(
                cacheKey = cacheKey,
                manifestUri = offlineManifestUri?.takeIf(String::isNotBlank),
                useRotatingHlsCacheKeys = useRotatingHlsCacheKeys,
            )
        }
        trackSelection = PlayerTrackSelectionConfig(audioTrackPolicy, isOfflinePlayback)
    }

    override fun trackSelectionConfig(): PlayerTrackSelectionConfig = trackSelection

    override fun dataSourceFactory(): DataSource.Factory = DataSource.Factory {
        val offline = offlineCacheConfig
        if (offline != null) {
            CacheDataSource.Factory()
                .setCache(downloadPlaybackCache.cache)
                .apply {
                    if (offline.useRotatingHlsCacheKeys) {
                        setCacheKeyFactory(
                            downloadPlaybackCache.rotatingHlsCacheKeyFactory(
                                downloadCacheKey = offline.cacheKey,
                                manifestUri = offline.manifestUri,
                            )
                        )
                    }
                }
                .setFlags(CacheDataSource.FLAG_BLOCK_ON_CACHE)
                .createDataSource()
        } else {
            CacheDataSource.Factory()
                .setCache(streamingCacheProvider.cache)
                .setUpstreamDataSourceFactory(PlayerDataSourceFactory.create(headers))
                .createDataSource()
        }
    }
}
