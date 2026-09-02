package su.afk.yummy.tv.android.di

import android.app.Application
import android.os.StrictMode
import androidx.annotation.OptIn
import androidx.hilt.work.HiltWorkerFactory
import androidx.media3.cast.Cast
import androidx.media3.cast.CastParams
import androidx.media3.common.util.UnstableApi
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import su.afk.yummy.tv.BuildConfig
import su.afk.yummy.tv.android.episodepush.NewEpisodePushScheduler
import su.afk.yummy.tv.android.lifecycle.OnlineStatusCoordinator
import su.afk.yummy.tv.android.outbox.AndroidPendingMutationSyncScheduler
import su.afk.yummy.tv.android.startup.AppStartupMaintenanceRunner
import su.afk.yummy.tv.android.startup.CoilImageLoaderInstaller
import su.afk.yummy.tv.core.analytics.api.initialize.AnalyticsInitializer
import su.afk.yummy.tv.core.featuretoggle.FeatureToggleRefreshCoordinator
import su.afk.yummy.tv.core.featuretoggle.api.FeatureToggleInitializer
import su.afk.yummy.tv.core.tv.HomeFeedRefreshScheduler
import javax.inject.Inject

@HiltAndroidApp
class YummyTvApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var homeFeedRefreshScheduler: HomeFeedRefreshScheduler

    @Inject
    lateinit var newEpisodePushScheduler: NewEpisodePushScheduler

    @Inject
    lateinit var pendingMutationSyncScheduler: AndroidPendingMutationSyncScheduler

    @Inject
    lateinit var analyticsInitializer: AnalyticsInitializer

    @Inject
    lateinit var featureToggleInitializer: FeatureToggleInitializer

    @Inject
    lateinit var onlineStatusCoordinator: OnlineStatusCoordinator

    @Inject
    lateinit var featureToggleRefreshCoordinator: FeatureToggleRefreshCoordinator

    @Inject
    lateinit var coilImageLoaderInstaller: CoilImageLoaderInstaller

    @Inject
    lateinit var startupMaintenanceRunner: AppStartupMaintenanceRunner

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    @OptIn(UnstableApi::class)
    override fun onCreate() {
        super.onCreate()

        installStrictModeIfDebug()
        setupAnalytics()
        setupFeatureToggles()
        setupCast()
        coilImageLoaderInstaller.install()
        onlineStatusCoordinator.start()
        featureToggleRefreshCoordinator.start()
        homeFeedRefreshScheduler.schedule()
        newEpisodePushScheduler.schedule()
        pendingMutationSyncScheduler.schedule()
        startupMaintenanceRunner.run()
    }

    private fun installStrictModeIfDebug() {
        if (!BuildConfig.DEBUG) return
        StrictMode.setThreadPolicy(
            StrictMode.ThreadPolicy.Builder()
                .detectDiskReads()
                .detectDiskWrites()
                .detectNetwork()
                .penaltyLog()
                .build()
        )
    }

    private fun setupAnalytics() {
        analyticsInitializer.initialize(this, BuildConfig.APPMETRICA_API_KEY)
    }

    private fun setupFeatureToggles() {
        featureToggleInitializer.initialize(this, BuildConfig.VARIOQUB_CLIENT_ID)
    }

    /**
     * Manifest-provided Cast options (см. YummyTvCastOptionsProvider) по умолчанию заставляют
     * media3-cast использовать устаревший in-app MediaRouteChooserDialog вместо системного Output
     * Switcher - на практике этот диалог не показывает часть реально найденных Cast-устройств,
     * хотя они уже есть в системной таблице MediaRouter2 (проверено логами и системный Cast/VLC
     * те же устройства находят). Явный showSystemOutputSwitcherOnCastButtonClick форсирует
     * системный picker независимо от этого дефолта, receiverApplicationId по-прежнему берётся из
     * манифеста.
     */
    @OptIn(UnstableApi::class)
    private fun setupCast() {
        Cast.getSingletonInstance(this).initialize(
            CastParams.Builder()
                .setShowSystemOutputSwitcherOnCastButtonClick(true)
                .build()
        )
    }
}
