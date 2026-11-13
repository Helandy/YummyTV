package su.afk.yummy.tv.android.startup

import android.content.Context
import android.os.StrictMode
import androidx.tracing.trace
import dagger.hilt.android.qualifiers.ApplicationContext
import su.afk.yummy.tv.BuildConfig
import su.afk.yummy.tv.android.cast.CastInitializer
import su.afk.yummy.tv.android.image.CoilImageLoaderInstaller
import su.afk.yummy.tv.android.lifecycle.OnlineStatusCoordinator
import su.afk.yummy.tv.android.seasonal.SeasonalAppIconSwitcher
import su.afk.yummy.tv.core.analytics.api.initialize.AnalyticsInitializer
import su.afk.yummy.tv.core.featuretoggle.FeatureToggleRefreshCoordinator
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Шаги холодного старта процесса в том порядке, в котором они должны выполниться в
 * Application.onCreate. Секции `App.*` видны в Perfetto и в TraceSectionMetric бенчмарка старта
 * (StartupBenchmark в :baselineprofile) — имена секций менять вместе с ним.
 */
@Singleton
class AppStartupRunner @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val analyticsInitializer: AnalyticsInitializer,
    private val remoteConfigInitializer: RemoteConfigInitializer,
    private val castInitializer: CastInitializer,
    private val coilImageLoaderInstaller: CoilImageLoaderInstaller,
    private val watchedEpisodeRuleSync: WatchedEpisodeRuleSync,
    private val onlineStatusCoordinator: OnlineStatusCoordinator,
    private val featureToggleRefreshCoordinator: FeatureToggleRefreshCoordinator,
    private val backgroundWorkStarter: BackgroundWorkStarter,
    private val startupMaintenanceRunner: AppStartupMaintenanceRunner,
    private val seasonalAppIconSwitcher: SeasonalAppIconSwitcher,
) {
    fun run() {
        installStrictModeIfDebug()
        trace("App.analytics") {
            analyticsInitializer.initialize(context, BuildConfig.APPMETRICA_API_KEY)
        }
        trace("App.featureToggles") { remoteConfigInitializer.initialize() }
        trace("App.cast") { castInitializer.initialize() }
        trace("App.coil") { coilImageLoaderInstaller.install() }
        trace("App.watchedEpisodeRuleSync") { watchedEpisodeRuleSync.start() }
        trace("App.onlineStatus") { onlineStatusCoordinator.start() }
        trace("App.featureToggleRefresh") { featureToggleRefreshCoordinator.start() }
        backgroundWorkStarter.start()
        trace("App.maintenance") { startupMaintenanceRunner.run() }
        trace("App.seasonalIcon") { seasonalAppIconSwitcher.start() }
    }

    private fun installStrictModeIfDebug() {
        if (!BuildConfig.DEBUG) return
        StrictMode.setThreadPolicy(
            StrictMode.ThreadPolicy.Builder()
                .detectDiskReads()
                .detectDiskWrites()
                .detectNetwork()
                .penaltyLog()
                .build(),
        )
    }
}
