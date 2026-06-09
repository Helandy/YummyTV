package su.afk.yummy.tv.android.di

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.annotation.ExperimentalCoilApi
import coil3.disk.DiskCache
import coil3.disk.directory
import coil3.memory.MemoryCache
import coil3.network.ktor3.KtorNetworkFetcherFactory
import coil3.request.crossfade
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import su.afk.yummy.tv.BuildConfig
import su.afk.yummy.tv.android.worker.HomeFeedRefreshScheduler
import su.afk.yummy.tv.core.preferences.settings.SettingsStore
import java.io.File
import javax.inject.Inject

@HiltAndroidApp
class YummyTvApplication : Application(), Configuration.Provider {

    @Inject lateinit var settingsStore: SettingsStore
    @Inject lateinit var workerFactory: HiltWorkerFactory
    @Inject lateinit var homeFeedRefreshScheduler: HomeFeedRefreshScheduler

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        setupCoilImageLoader()
        homeFeedRefreshScheduler.schedule()
        applicationScope.launch {
            if (settingsStore.markStartedVersion(BuildConfig.VERSION_CODE)) {
                deleteDownloadedUpdateApk()
            }
        }
    }

    private fun deleteDownloadedUpdateApk() {
        File(cacheDir, UPDATE_APK_FILE_NAME).delete()
    }

    @OptIn(ExperimentalCoilApi::class)
    private fun setupCoilImageLoader() {
        val cacheBytes = settingsStore.currentPreviewCacheSize.megabytes.toLong() * 1024L * 1024L
        SingletonImageLoader.setSafe {
            ImageLoader.Builder(it)
                .crossfade(true)
                .memoryCache {
                    MemoryCache.Builder()
                        .maxSizePercent(applicationContext, 0.20)
                        .build()
                }
                .diskCache {
                    DiskCache.Builder()
                        .directory(cacheDir.resolve(IMAGE_CACHE_DIR_NAME))
                        .maxSizeBytes(cacheBytes)
                        .build()
                }
                .components { add(KtorNetworkFetcherFactory()) }
                .build()
        }
    }

    private companion object {
        const val UPDATE_APK_FILE_NAME = "update.apk"
        private const val IMAGE_CACHE_DIR_NAME = "image_cache"
    }
}
