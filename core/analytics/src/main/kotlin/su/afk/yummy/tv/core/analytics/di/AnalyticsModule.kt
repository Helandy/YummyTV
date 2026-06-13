package su.afk.yummy.tv.core.analytics.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import su.afk.yummy.tv.core.analytics.AnalyticsInitializer
import su.afk.yummy.tv.core.analytics.AnalyticsTracker
import su.afk.yummy.tv.core.analytics.AppMetricaAnalyticsInitializer
import su.afk.yummy.tv.core.analytics.AppMetricaAnalyticsTracker
import su.afk.yummy.tv.core.analytics.BuildConfig
import su.afk.yummy.tv.core.analytics.LogcatAnalyticsTracker
import su.afk.yummy.tv.core.analytics.NoOpAnalyticsInitializer
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
}
