package su.afk.yummy.tv.data.videodownload.cache

import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import su.afk.yummy.tv.core.storage.videodownload.VideoDownloadStore
import javax.inject.Inject

/**
 * One-time cleanup for installs made before regular (non-offline) playback got its own bounded
 * cache. Until then, every streamed video was written into the unbounded download cache
 * ([VideoDownloadCacheProvider]) and never evicted. This removes everything from that cache that
 * isn't backed by an active download entry, leaving user-initiated downloads untouched.
 */
@OptIn(UnstableApi::class)
class LegacyStreamingCachePruner @Inject constructor(
    private val cacheProvider: VideoDownloadCacheProvider,
    private val store: VideoDownloadStore,
) {
    suspend fun pruneOrphanedEntries() {
        val activeCacheKeys = store.dao.getActiveCacheKeys().toSet()
        val activeSegmentPrefixes = activeCacheKeys.map(RotatingHlsCacheKeyFactory::resourcePrefix)
        cacheProvider.cache.keys
            .filterNot { key -> key in activeCacheKeys || activeSegmentPrefixes.any(key::startsWith) }
            .forEach { key -> runCatching { cacheProvider.cache.removeResource(key) } }
    }
}
