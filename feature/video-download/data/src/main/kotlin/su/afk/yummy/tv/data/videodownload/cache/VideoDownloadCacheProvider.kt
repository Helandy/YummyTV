package su.afk.yummy.tv.data.videodownload.cache

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.cache.NoOpCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@OptIn(UnstableApi::class)
@Singleton
class VideoDownloadCacheProvider @Inject constructor(
    @ApplicationContext context: Context,
) {
    val cache: SimpleCache by lazy {
        SimpleCache(
            File(context.filesDir, CACHE_DIR_NAME),
            NoOpCacheEvictor(),
            StandaloneDatabaseProvider(context),
        )
    }

    private companion object {
        const val CACHE_DIR_NAME = "video_download_cache"
    }
}
