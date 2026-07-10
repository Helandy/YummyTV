package su.afk.yummy.tv.feature.player.service

import androidx.media3.datasource.DataSource
import androidx.media3.datasource.cache.CacheDataSource
import su.afk.yummy.tv.data.videodownload.cache.RotatingHlsCacheKeyFactory
import su.afk.yummy.tv.data.videodownload.cache.VideoDownloadCacheProvider
import su.afk.yummy.tv.feature.player.common.PlayerDataSourceFactory
import javax.inject.Inject
import javax.inject.Singleton

internal interface MobilePlayerPlaybackConfig {
    fun updateStream(
        headers: Map<String, String>,
        offlineCacheKey: String?,
        useRotatingHlsCacheKeys: Boolean,
    )
    fun dataSourceFactory(): DataSource.Factory
}

@Singleton
internal class DefaultMobilePlayerPlaybackConfig @Inject constructor(
    private val cacheProvider: VideoDownloadCacheProvider,
) :
    MobilePlayerPlaybackConfig {
    @Volatile
    private var streamHeaders: Map<String, String> = emptyMap()

    @Volatile
    private var offlineCacheKey: String? = null

    @Volatile
    private var useRotatingHlsCacheKeys: Boolean = false

    override fun updateStream(
        headers: Map<String, String>,
        offlineCacheKey: String?,
        useRotatingHlsCacheKeys: Boolean,
    ) {
        streamHeaders = headers.toMap()
        this.offlineCacheKey = offlineCacheKey
        this.useRotatingHlsCacheKeys = useRotatingHlsCacheKeys
    }

    override fun dataSourceFactory(): DataSource.Factory =
        object : DataSource.Factory {
            override fun createDataSource(): DataSource {
                val cache = cacheProvider.cache
                val key = offlineCacheKey
                return if (key != null) {
                    CacheDataSource.Factory()
                        .setCache(cache)
                        .apply {
                            if (useRotatingHlsCacheKeys) {
                                setCacheKeyFactory(RotatingHlsCacheKeyFactory(key))
                            }
                        }
                        .setFlags(CacheDataSource.FLAG_BLOCK_ON_CACHE)
                        .createDataSource()
                } else {
                    CacheDataSource.Factory()
                        .setCache(cache)
                        .setUpstreamDataSourceFactory(PlayerDataSourceFactory.create(streamHeaders))
                        .createDataSource()
                }
            }
        }
}
