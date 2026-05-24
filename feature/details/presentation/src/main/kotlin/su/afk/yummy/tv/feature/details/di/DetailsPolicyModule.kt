package su.afk.yummy.tv.feature.details.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import su.afk.yummy.tv.core.storage.settings.SettingsStore
import su.afk.yummy.tv.domain.anime.FirstLaunchTimestampProvider
import su.afk.yummy.tv.domain.anime.IsAnimeRegionBlockedUseCase
import javax.inject.Named

@Module
@InstallIn(SingletonComponent::class)
object DetailsPolicyModule {

    @Provides
    fun provideFirstLaunchTimestampProvider(settingsStore: SettingsStore): FirstLaunchTimestampProvider =
        SettingsFirstLaunchTimestampProvider(settingsStore)

    @Provides
    fun provideIsAnimeRegionBlockedUseCase(
        @Named("blockedTimeoutEnabled") blockedTimeoutEnabled: Boolean,
        firstLaunchTimestampProvider: FirstLaunchTimestampProvider,
    ): IsAnimeRegionBlockedUseCase =
        IsAnimeRegionBlockedUseCase(
            blockedTimeoutEnabled = blockedTimeoutEnabled,
            firstLaunchTimestampProvider = firstLaunchTimestampProvider,
        )
}

private class SettingsFirstLaunchTimestampProvider(
    private val settingsStore: SettingsStore,
) : FirstLaunchTimestampProvider {
    override suspend fun getOrCreateFirstLaunchAtMillis(): Long =
        settingsStore.ensureFirstLaunchAt()
}
