package su.afk.yummy.tv.data.videodownload.cache

import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.cache.Cache
import androidx.media3.datasource.cache.CacheKeyFactory
import su.afk.yummy.tv.feature.videodownload.playback.VideoDownloadPlaybackCache
import javax.inject.Inject
import javax.inject.Singleton

@OptIn(UnstableApi::class)
@Singleton
class DefaultVideoDownloadPlaybackCache @Inject constructor(
    private val cacheProvider: VideoDownloadCacheProvider,
) : VideoDownloadPlaybackCache {

    override val cache: Cache
        get() = cacheProvider.cache

    override fun rotatingHlsCacheKeyFactory(
        downloadCacheKey: String,
        manifestUri: String?,
    ): CacheKeyFactory = RotatingHlsCacheKeyFactory(
        downloadCacheKey = downloadCacheKey,
        manifestUri = manifestUri,
    )
}
