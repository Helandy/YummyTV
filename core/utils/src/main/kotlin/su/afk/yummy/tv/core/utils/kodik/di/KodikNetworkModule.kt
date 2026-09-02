package su.afk.yummy.tv.core.utils.kodik.di

import android.content.Context
import coil3.disk.DiskCache
import coil3.disk.directory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.compression.ContentEncoding
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object KodikNetworkModule {

    @Provides
    @Singleton
    @KodikHttpClient
    fun provideKodikHttpClient(): HttpClient = HttpClient(OkHttp) {
        install(HttpTimeout)
        install(ContentEncoding) {
            gzip()
            deflate()
        }
    }

    /**
     * Отдельный маленький disk cache только для Kodik-превью серий: свой лимит, не зависящий
     * от пользовательской настройки общего кэша картинок (см. CoilImageLoaderInstaller).
     */
    @Provides
    @Singleton
    @KodikThumbnailDiskCache
    fun provideKodikThumbnailDiskCache(@ApplicationContext context: Context): DiskCache =
        DiskCache.Builder()
            .directory(context.cacheDir.resolve(THUMBNAIL_CACHE_DIR_NAME))
            .maxSizeBytes(THUMBNAIL_DISK_CACHE_BYTES)
            .build()

    private const val THUMBNAIL_CACHE_DIR_NAME = "kodik_thumbnail_cache"
    private const val THUMBNAIL_DISK_CACHE_BYTES = 15L * 1024 * 1024
}
