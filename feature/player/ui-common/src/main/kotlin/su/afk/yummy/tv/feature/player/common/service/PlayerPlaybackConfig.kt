package su.afk.yummy.tv.feature.player.common.service

import android.content.Context
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.cache.CacheDataSource
import dagger.hilt.android.qualifiers.ApplicationContext
import su.afk.yummy.tv.feature.player.common.PlayerDataSourceFactory
import su.afk.yummy.tv.feature.videodownload.playback.VideoDownloadPlaybackCache
import javax.inject.Inject
import javax.inject.Singleton

interface PlayerPlaybackConfig {
    fun updateStream(
        headers: Map<String, String>,
        offlineCacheKey: String?,
        offlineManifestUri: String?,
        offlineCacheKeyScheme: Int,
        useRotatingHlsCacheKeys: Boolean,
        audioTrackPolicy: PlayerAudioTrackPolicy,
        isOfflinePlayback: Boolean,
        isLocalFile: Boolean,
        silentReconnectEnabled: Boolean,
    )

    fun dataSourceFactory(): DataSource.Factory
    fun trackSelectionConfig(): PlayerTrackSelectionConfig

    /**
     * Включено ли «тихое переподключение под буфер» для текущего источника — продлённое окно
     * фоновых ретраев загрузчика ([PlayerLoadErrorHandlingPolicy]). Выключено для Alloha
     * (у неё свой fresh-session recovery) и для офлайна/локальных файлов.
     */
    fun silentReconnectEnabled(): Boolean
}

data class PlayerTrackSelectionConfig(
    val audioTrackPolicy: PlayerAudioTrackPolicy = PlayerAudioTrackPolicy.Default,
    val isOfflinePlayback: Boolean = false,
)

private data class OfflineCacheConfig(
    val cacheKey: String,
    val manifestUri: String?,
    val cacheKeyScheme: Int,
    val useRotatingHlsCacheKeys: Boolean,
)

@Singleton
class DefaultPlayerPlaybackConfig @Inject constructor(
    @ApplicationContext private val context: Context,
    private val downloadPlaybackCache: VideoDownloadPlaybackCache,
    private val streamingCacheProvider: PlayerStreamingCacheProvider,
    private val httpDataSourceFactory: PlayerDataSourceFactory,
) : PlayerPlaybackConfig {
    @Volatile
    private var headers: Map<String, String> = emptyMap()

    @Volatile
    private var offlineCacheConfig: OfflineCacheConfig? = null

    @Volatile
    private var isLocalFile: Boolean = false

    @Volatile
    private var trackSelection = PlayerTrackSelectionConfig()

    @Volatile
    private var silentReconnectEnabled: Boolean = false

    override fun updateStream(
        headers: Map<String, String>,
        offlineCacheKey: String?,
        offlineManifestUri: String?,
        offlineCacheKeyScheme: Int,
        useRotatingHlsCacheKeys: Boolean,
        audioTrackPolicy: PlayerAudioTrackPolicy,
        isOfflinePlayback: Boolean,
        isLocalFile: Boolean,
        silentReconnectEnabled: Boolean,
    ) {
        this.headers = headers.toMap()
        this.isLocalFile = isLocalFile
        this.silentReconnectEnabled = silentReconnectEnabled
        offlineCacheConfig = offlineCacheKey?.let { cacheKey ->
            OfflineCacheConfig(
                cacheKey = cacheKey,
                manifestUri = offlineManifestUri?.takeIf(String::isNotBlank),
                cacheKeyScheme = offlineCacheKeyScheme,
                useRotatingHlsCacheKeys = useRotatingHlsCacheKeys,
            )
        }
        trackSelection = PlayerTrackSelectionConfig(audioTrackPolicy, isOfflinePlayback)
    }

    override fun trackSelectionConfig(): PlayerTrackSelectionConfig = trackSelection

    override fun silentReconnectEnabled(): Boolean = silentReconnectEnabled

    override fun dataSourceFactory(): DataSource.Factory = DataSource.Factory {
        val offline = offlineCacheConfig
        if (isLocalFile) {
            // Локальный файл (content://, file://): без кэша, DefaultDataSource
            // сам обслуживает content/file/asset/rawresource-схемы.
            DefaultDataSource.Factory(context).createDataSource()
        } else if (offline != null) {
            CacheDataSource.Factory()
                .setCache(downloadPlaybackCache.cache)
                .apply {
                    downloadPlaybackCache.downloadCacheKeyFactory(
                        downloadCacheKey = offline.cacheKey,
                        manifestUri = offline.manifestUri,
                        cacheKeyScheme = offline.cacheKeyScheme,
                        legacyRotating = offline.useRotatingHlsCacheKeys,
                    )?.let(::setCacheKeyFactory)
                }
                .setFlags(CacheDataSource.FLAG_BLOCK_ON_CACHE)
                .createDataSource()
        } else {
            CacheDataSource.Factory()
                .setCache(streamingCacheProvider.cache)
                .setCacheKeyFactory(PlayerStreamingCacheKeyFactory())
                .setUpstreamDataSourceFactory(httpDataSourceFactory.create(headers))
                .createDataSource()
        }
    }
}
