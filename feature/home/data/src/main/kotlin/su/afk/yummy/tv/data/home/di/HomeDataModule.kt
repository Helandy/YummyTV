package su.afk.yummy.tv.data.home.di

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import su.afk.yummy.tv.core.analytics.api.AnalyticsTracker
import su.afk.yummy.tv.core.error.api.StringProvider
import su.afk.yummy.tv.core.network.yani.YaniHttpClientProvider
import su.afk.yummy.tv.core.preferences.api.YaniAccountSettingsStore
import su.afk.yummy.tv.core.preferences.settings.PlayerSettingsStore
import su.afk.yummy.tv.core.storage.anime.AnimeStorage
import su.afk.yummy.tv.core.storage.document.DocumentCacheStorage
import su.afk.yummy.tv.core.storage.home.HomeFeedStorage
import su.afk.yummy.tv.core.storage.watchprogress.WatchProgressStorage
import su.afk.yummy.tv.core.tv.api.TvChannelContentProvider
import su.afk.yummy.tv.data.home.network.YaniHomeApi
import su.afk.yummy.tv.data.home.repository.YaniHomeFeedRepository
import su.afk.yummy.tv.data.home.repository.YaniRemoteContinueWatchingRepository
import su.afk.yummy.tv.data.home.tv.HomeTvChannelContentProvider
import su.afk.yummy.tv.domain.home.repository.HomeFeedRepository
import su.afk.yummy.tv.domain.home.repository.RemoteContinueWatchingRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface HomeTvIntegrationModule {

    @Binds
    @Singleton
    fun bindTvChannelContentProvider(
        impl: HomeTvChannelContentProvider,
    ): TvChannelContentProvider
}

@Module
@InstallIn(SingletonComponent::class)
object HomeDataModule {

    @Provides
    @Singleton
    fun provideYaniHomeApi(clientProvider: YaniHttpClientProvider): YaniHomeApi =
        YaniHomeApi(clientProvider)

    @Provides
    @Singleton
    fun provideHomeFeedRepository(
        api: YaniHomeApi,
        homeFeedStore: HomeFeedStorage,
        stringProvider: StringProvider,
        settingsStore: YaniAccountSettingsStore,
        playerSettingsStore: PlayerSettingsStore,
        watchProgressStore: WatchProgressStorage,
        animeStorage: AnimeStorage,
        analyticsTracker: AnalyticsTracker,
    ): HomeFeedRepository =
        YaniHomeFeedRepository(
            api,
            homeFeedStore,
            stringProvider,
            settingsStore,
            playerSettingsStore,
            watchProgressStore,
            animeStorage,
            analyticsTracker,
        )

    @Provides
    @Singleton
    fun provideRemoteContinueWatchingRepository(
        api: YaniHomeApi,
        documentCache: DocumentCacheStorage,
        settingsStore: YaniAccountSettingsStore,
        analyticsTracker: AnalyticsTracker,
    ): RemoteContinueWatchingRepository =
        YaniRemoteContinueWatchingRepository(api, documentCache, settingsStore, analyticsTracker)
}
