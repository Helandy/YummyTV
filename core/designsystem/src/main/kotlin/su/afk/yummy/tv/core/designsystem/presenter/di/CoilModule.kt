package su.afk.yummy.tv.core.designsystem.presenter.di

import android.content.Context
import coil3.ImageLoader
import coil3.annotation.ExperimentalCoilApi
import coil3.disk.DiskCache
import coil3.disk.directory
import coil3.memory.MemoryCache
import coil3.network.ktor3.KtorNetworkFetcherFactory
import coil3.request.crossfade
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import su.afk.yummy.tv.core.storage.settings.SettingsStore
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object CoilModule {

    /** Превью картинок */
    @Provides
    @Singleton
    @OptIn(ExperimentalCoilApi::class)
    fun provideImageLoader(
        @ApplicationContext appContext: Context,
        settingsStore: SettingsStore,
    ): ImageLoader {
        val cacheMb = runBlocking { settingsStore.previewCacheSize.first() }.megabytes

        val cacheBytes = cacheMb.toLong() * 1024L * 1024L

        return ImageLoader.Builder(appContext)
            .crossfade(true)
            .memoryCache {
                MemoryCache.Builder()
                    .maxSizePercent(appContext, 0.20)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(appContext.cacheDir.resolve(IMAGE_VIEW_COIL_DISK_DIR_NAME))
                    .maxSizeBytes(cacheBytes)
                    .build()
            }
            .components { add(KtorNetworkFetcherFactory()) }
            .build()
    }

    private const val IMAGE_VIEW_COIL_DISK_DIR_NAME = "image_cache"

}