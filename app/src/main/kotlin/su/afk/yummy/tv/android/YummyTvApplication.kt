package su.afk.yummy.tv.android

import android.app.Application
import android.content.Context
import android.os.SystemClock
import androidx.hilt.work.HiltWorkerFactory
import androidx.tracing.trace
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import su.afk.yummy.tv.android.startup.AppStartupRunner
import su.afk.yummy.tv.android.startup.metrics.StartupMetricsTracker
import su.afk.yummy.tv.core.model.anime.utils.PosterMirrorConfig
import su.afk.yummy.tv.core.preferences.interface_language.AppInterfaceLanguageContext
import su.afk.yummy.tv.core.utils.logging.AppLogCollector
import javax.inject.Inject
import su.afk.yummy.tv.core.network.BuildConfig as NetworkBuildConfig

@HiltAndroidApp
class YummyTvApplication :
    Application(),
    Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var appLogCollector: AppLogCollector

    @Inject
    lateinit var startupMetricsTracker: StartupMetricsTracker

    @Inject
    lateinit var appStartupRunner: AppStartupRunner

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(AppInterfaceLanguageContext.wrap(base))
    }

    override fun onCreate() {
        // Hilt инжектит поля в super.onCreate(), поэтому начало замеряем до него
        val onCreateStartedAt = SystemClock.uptimeMillis()
        trace("App.hiltInject") { super.onCreate() }
        PosterMirrorConfig.host = NetworkBuildConfig.STATIC_HOST
        // Файловый дамп logcat для передачи разработчику (Настройки → О приложении)
        appLogCollector.start()
        startupMetricsTracker.start(this, onCreateStartedAt)

        appStartupRunner.run()

        startupMetricsTracker.onApplicationCreated()
    }
}
