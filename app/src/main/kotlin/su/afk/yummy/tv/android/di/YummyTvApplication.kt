package su.afk.yummy.tv.android.di

import android.app.ActivityManager
import android.app.Application
import android.os.StrictMode
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
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import su.afk.yummy.tv.BuildConfig
import su.afk.yummy.tv.android.worker.HomeFeedRefreshScheduler
import su.afk.yummy.tv.core.analytics.AnalyticsInitializer
import su.afk.yummy.tv.core.featuretoggle.FeatureToggleInitializer
import su.afk.yummy.tv.core.preferences.settings.SettingsStore
import su.afk.yummy.tv.data.videodownload.cache.LegacyStreamingCachePruner
import java.io.File
import javax.inject.Inject

@HiltAndroidApp
class YummyTvApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var settingsStore: SettingsStore

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var homeFeedRefreshScheduler: HomeFeedRefreshScheduler

    @Inject
    lateinit var analyticsInitializer: AnalyticsInitializer

    @Inject
    lateinit var featureToggleInitializer: FeatureToggleInitializer

    @Inject
    lateinit var legacyStreamingCachePruner: LegacyStreamingCachePruner

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()

        if (BuildConfig.DEBUG) {
            StrictMode.setThreadPolicy(
                StrictMode.ThreadPolicy.Builder()
                    .detectDiskReads()
                    .detectDiskWrites()
                    .detectNetwork()
                    .penaltyLog()
                    .build()
            )
        }

        setupAnalytics()
        setupFeatureToggles()
        setupCoilImageLoader()
        homeFeedRefreshScheduler.schedule()
        applicationScope.launch {
            settingsStore.ensureYaniContentLanguageInitialized()
            if (settingsStore.markStartedVersion(BuildConfig.VERSION_CODE)) {
                deleteDownloadedUpdateApk()
            }
            if (settingsStore.consumeLegacyStreamingCachePruneFlag()) {
                runCatching { legacyStreamingCachePruner.pruneOrphanedEntries() }
            }
        }
    }

    private fun deleteDownloadedUpdateApk() {
        File(cacheDir, UPDATE_APK_FILE_NAME).delete()
    }

    private fun isLowRamDevice(): Boolean =
        (getSystemService(ACTIVITY_SERVICE) as? ActivityManager)?.isLowRamDevice == true

    private fun setupAnalytics() {
        analyticsInitializer.initialize(this, BuildConfig.APPMETRICA_API_KEY)
    }

    private fun setupFeatureToggles() {
        featureToggleInitializer.initialize(this, BuildConfig.VARIOQUB_CLIENT_ID)
    }

    @OptIn(ExperimentalCoilApi::class)
    private fun setupCoilImageLoader() {
        val cacheBytes = settingsStore.currentPreviewCacheSize.megabytes.toLong() * 1024L * 1024L
        val memoryCachePercent =
            if (isLowRamDevice()) LOW_RAM_MEMORY_CACHE_PERCENT else MEMORY_CACHE_PERCENT
        SingletonImageLoader.setSafe {
            val imageHttpClient = HttpClient(OkHttp)
            ImageLoader.Builder(it)
                .crossfade(true)
                .memoryCache {
                    MemoryCache.Builder()
                        .maxSizePercent(applicationContext, memoryCachePercent)
                        .build()
                }
                .diskCache {
                    DiskCache.Builder()
                        .directory(cacheDir.resolve(IMAGE_CACHE_DIR_NAME))
                        .maxSizeBytes(cacheBytes)
                        .build()
                }
                .components {
                    add(KtorNetworkFetcherFactory(httpClient = imageHttpClient))
                }
                .build()
        }
    }

    private companion object {
        const val UPDATE_APK_FILE_NAME = "update.apk"
        private const val IMAGE_CACHE_DIR_NAME = "image_cache"
        private const val MEMORY_CACHE_PERCENT = 0.15
        private const val LOW_RAM_MEMORY_CACHE_PERCENT = 0.10
    }
}
