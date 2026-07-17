package su.afk.yummy.tv.feature.player.common.service

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Bounded on-disk cache for regular (non-offline) playback, kept separate from
 * [su.afk.yummy.tv.feature.videodownload.playback.VideoDownloadPlaybackCache], whose cache is
 * unbounded and reserved for user-initiated downloads.
 */
@OptIn(UnstableApi::class)
@Singleton
class PlayerStreamingCacheProvider @Inject constructor(
    @ApplicationContext context: Context,
) {
    val cache: SimpleCache by lazy {
        SimpleCache(
            File(context.cacheDir, CACHE_DIR_NAME),
            LeastRecentlyUsedCacheEvictor(MAX_CACHE_BYTES),
            StandaloneDatabaseProvider(context),
        )
    }

    private companion object {
        const val CACHE_DIR_NAME = "player_streaming_cache"
        const val MAX_CACHE_BYTES = 50L * 1024 * 1024
    }
}
