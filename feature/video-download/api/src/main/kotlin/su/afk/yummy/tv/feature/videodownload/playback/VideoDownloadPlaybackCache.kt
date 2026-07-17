package su.afk.yummy.tv.feature.videodownload.playback

import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.cache.Cache
import androidx.media3.datasource.cache.CacheKeyFactory

/**
 * Playback-facing contract of the video download cache: gives the player access to the
 * offline downloads cache without exposing the video-download data layer.
 */
@OptIn(UnstableApi::class)
interface VideoDownloadPlaybackCache {

    /** Cache holding user-initiated downloads. */
    val cache: Cache

    /**
     * Key factory for HLS downloads whose signed URLs rotate: keeps already downloaded
     * segments reusable when the manifest URL changes.
     */
    fun rotatingHlsCacheKeyFactory(downloadCacheKey: String, manifestUri: String?): CacheKeyFactory
}
