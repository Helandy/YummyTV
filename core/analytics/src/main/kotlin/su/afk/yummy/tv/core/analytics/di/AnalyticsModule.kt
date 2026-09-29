package su.afk.yummy.tv.core.analytics.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import su.afk.yummy.tv.core.analytics.BuildConfig
import su.afk.yummy.tv.core.analytics.api.AnalyticsDeviceIdProvider
import su.afk.yummy.tv.core.analytics.api.AnalyticsTracker
import su.afk.yummy.tv.core.analytics.api.coroutine.ErrorCoroutineAnalytics
import su.afk.yummy.tv.core.analytics.api.initialize.AnalyticsInitializer
import su.afk.yummy.tv.core.analytics.appmetrica.AppMetricaAnalyticsInitializer
import su.afk.yummy.tv.core.analytics.appmetrica.AppMetricaAnalyticsTracker
import su.afk.yummy.tv.core.analytics.appmetrica.AppMetricaDeviceIdProvider
import su.afk.yummy.tv.core.analytics.coroutine.ErrorCoroutineAnalyticsImpl
import su.afk.yummy.tv.core.analytics.logcat.LogcatAnalyticsTracker
import su.afk.yummy.tv.core.analytics.logcat.NoOpAnalyticsDeviceIdProvider
import su.afk.yummy.tv.core.analytics.logcat.NoOpAnalyticsInitializer
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object AnalyticsModule {

    @Provides
    @Singleton
    fun provideAnalyticsTracker(
        appMetricaAnalyticsTracker: AppMetricaAnalyticsTracker,
        logcatAnalyticsTracker: LogcatAnalyticsTracker,
    ): AnalyticsTracker =
        if (BuildConfig.DEBUG) logcatAnalyticsTracker else appMetricaAnalyticsTracker

    @Provides
    @Singleton
    fun provideAnalyticsInitializer(
        appMetricaAnalyticsInitializer: AppMetricaAnalyticsInitializer,
        noOpAnalyticsInitializer: NoOpAnalyticsInitializer,
    ): AnalyticsInitializer =
        if (BuildConfig.DEBUG) noOpAnalyticsInitializer else appMetricaAnalyticsInitializer

    @Provides
    @Singleton
    fun provideAnalyticsDeviceIdProvider(
        appMetricaDeviceIdProvider: AppMetricaDeviceIdProvider,
        noOpAnalyticsDeviceIdProvider: NoOpAnalyticsDeviceIdProvider,
    ): AnalyticsDeviceIdProvider =
        if (BuildConfig.DEBUG) noOpAnalyticsDeviceIdProvider else appMetricaDeviceIdProvider

    @Provides
    @Singleton
    fun provideErrorAnalyticsReporter(
        errorCoroutineAnalyticsReporterImpl: ErrorCoroutineAnalyticsImpl,
    ): ErrorCoroutineAnalytics = errorCoroutineAnalyticsReporterImpl
}
