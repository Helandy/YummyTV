package su.afk.yummy.tv.feature.player.common.service

import androidx.media3.datasource.DataSource
import androidx.media3.datasource.cache.CacheDataSource
import su.afk.yummy.tv.data.videodownload.cache.RotatingHlsCacheKeyFactory
import su.afk.yummy.tv.data.videodownload.cache.VideoDownloadCacheProvider
import su.afk.yummy.tv.feature.player.common.PlayerDataSourceFactory
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
    private val cacheProvider: VideoDownloadCacheProvider,
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
                .setCache(cacheProvider.cache)
                .apply {
                    if (offline.useRotatingHlsCacheKeys) {
                        setCacheKeyFactory(
                            RotatingHlsCacheKeyFactory(
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
                .setCache(cacheProvider.cache)
                .setUpstreamDataSourceFactory(PlayerDataSourceFactory.create(headers))
                .createDataSource()
        }
    }
}
