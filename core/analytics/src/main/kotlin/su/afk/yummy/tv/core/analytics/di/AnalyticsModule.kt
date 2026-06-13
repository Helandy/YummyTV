package su.afk.yummy.tv.core.analytics.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import su.afk.yummy.tv.core.analytics.AnalyticsInitializer
import su.afk.yummy.tv.core.analytics.AnalyticsTracker
import su.afk.yummy.tv.core.analytics.AppMetricaAnalyticsInitializer
import su.afk.yummy.tv.core.analytics.AppMetricaAnalyticsTracker
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface AnalyticsModule {

    @Binds
    @Singleton
    fun bindAnalyticsTracker(impl: AppMetricaAnalyticsTracker): AnalyticsTracker

    @Binds
    @Singleton
    fun bindAnalyticsInitializer(impl: AppMetricaAnalyticsInitializer): AnalyticsInitializer
}
