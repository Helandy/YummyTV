package su.afk.yummy.tv.data.home.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.ktor.client.HttpClient
import su.afk.yummy.tv.core.error.StringProvider
import su.afk.yummy.tv.core.preferences.settings.SettingsStore
import su.afk.yummy.tv.core.storage.home.HomeFeedStore
import su.afk.yummy.tv.core.storage.watchprogress.RemoteContinueWatchingStore
import su.afk.yummy.tv.core.storage.watchprogress.WatchProgressStore
import su.afk.yummy.tv.data.home.network.YaniHomeApi
import su.afk.yummy.tv.data.home.repository.ContinueWatchingEnricher
import su.afk.yummy.tv.data.home.repository.YaniHomeFeedRepository
import su.afk.yummy.tv.domain.home.repository.HomeFeedRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object HomeDataModule {

    @Provides
    @Singleton
    fun provideYaniHomeApi(client: HttpClient): YaniHomeApi = YaniHomeApi(client)

    @Provides
    @Singleton
    fun provideHomeFeedRepository(
        api: YaniHomeApi,
        homeFeedStore: HomeFeedStore,
        stringProvider: StringProvider,
        settingsStore: SettingsStore,
        watchProgressStore: WatchProgressStore,
        remoteContinueWatchingStore: RemoteContinueWatchingStore,
        continueWatchingEnricher: ContinueWatchingEnricher,
    ): HomeFeedRepository =
        YaniHomeFeedRepository(
            api,
            homeFeedStore,
            stringProvider,
            settingsStore,
            watchProgressStore,
            remoteContinueWatchingStore,
            continueWatchingEnricher,
        )
}
