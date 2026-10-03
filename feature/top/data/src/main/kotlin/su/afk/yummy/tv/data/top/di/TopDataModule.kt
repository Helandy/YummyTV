package su.afk.yummy.tv.data.top.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import su.afk.yummy.tv.core.network.yani.YaniHttpClientProvider
import su.afk.yummy.tv.core.preferences.settings.YaniAccountSettingsStore
import su.afk.yummy.tv.core.storage.top.AnimeTopStorage
import su.afk.yummy.tv.data.top.network.YaniAnimeTopApi
import su.afk.yummy.tv.data.top.repository.YaniAnimeTopRepository
import su.afk.yummy.tv.domain.top.repository.AnimeTopRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object TopDataModule {

    @Provides
    @Singleton
    fun provideYaniAnimeTopApi(clientProvider: YaniHttpClientProvider): YaniAnimeTopApi =
        YaniAnimeTopApi(clientProvider)

    @Provides
    @Singleton
    fun provideAnimeTopRepository(
        api: YaniAnimeTopApi,
        topStore: AnimeTopStorage,
        settingsStore: YaniAccountSettingsStore,
    ): AnimeTopRepository =
        YaniAnimeTopRepository(api, topStore, settingsStore)
}
