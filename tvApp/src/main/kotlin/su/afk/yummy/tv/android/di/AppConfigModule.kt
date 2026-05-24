package su.afk.yummy.tv.android.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import su.afk.yummy.tv.BuildConfig
import javax.inject.Named

@Module
@InstallIn(SingletonComponent::class)
object AppConfigModule {

    @Provides
    @Named("blockedTimeoutEnabled")
    fun provideBlockedTimeoutEnabled(): Boolean = BuildConfig.BLOCKED_TIMEOUT
}
