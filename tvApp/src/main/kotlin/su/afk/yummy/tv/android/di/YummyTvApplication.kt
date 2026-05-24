package su.afk.yummy.tv.android.di

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import su.afk.yummy.tv.BuildConfig
import su.afk.yummy.tv.core.storage.settings.SettingsStore
import java.io.File
import javax.inject.Inject

@HiltAndroidApp
class YummyTvApplication : Application() {

    @Inject lateinit var settingsStore: SettingsStore

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        applicationScope.launch {
            if (settingsStore.markStartedVersion(BuildConfig.VERSION_CODE)) {
                deleteDownloadedUpdateApk()
            }
        }
        if (BuildConfig.BLOCKED_TIMEOUT) {
            applicationScope.launch {
                settingsStore.ensureFirstLaunchAt()
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
