package su.afk.yummy.tv.data.details.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import su.afk.yummy.tv.core.network.YaniHttpClientProvider
import su.afk.yummy.tv.core.preferences.settings.SettingsStore
import su.afk.yummy.tv.core.storage.account.AccountStorageStore
import su.afk.yummy.tv.core.storage.anime.AnimeStorageStore
import su.afk.yummy.tv.core.storage.document.DocumentCacheStore
import su.afk.yummy.tv.core.storage.watchprogress.WatchProgressStore
import su.afk.yummy.tv.data.details.network.YaniAnimeApi
import su.afk.yummy.tv.data.details.repository.YaniAnimeRepository
import su.afk.yummy.tv.domain.anime.repository.AnimeRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DetailsDataModule {

    @Provides
    @Singleton
    fun provideYaniAnimeApi(clientProvider: YaniHttpClientProvider): YaniAnimeApi =
        YaniAnimeApi(clientProvider)

    @Provides
    @Singleton
    fun provideAnimeRepository(
        api: YaniAnimeApi,
        animeStorage: AnimeStorageStore,
        accountStorage: AccountStorageStore,
        settingsStore: SettingsStore,
        watchProgressStore: WatchProgressStore,
        documentCache: DocumentCacheStore,
    ): AnimeRepository =
        YaniAnimeRepository(
            api,
            animeStorage,
            accountStorage,
            settingsStore,
            watchProgressStore,
            documentCache,
        )
}
