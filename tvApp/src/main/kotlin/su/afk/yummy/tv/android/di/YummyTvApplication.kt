package su.afk.yummy.tv.android.di

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
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

    private companion object {
        const val UPDATE_APK_FILE_NAME = "update.apk"
    }
}
