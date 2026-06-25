package su.afk.yummy.tv.data.top.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import su.afk.yummy.tv.core.network.YaniHttpClientProvider
import su.afk.yummy.tv.core.preferences.settings.SettingsStore
import su.afk.yummy.tv.core.storage.top.AnimeTopStore
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
        topStore: AnimeTopStore,
        settingsStore: SettingsStore,
    ): AnimeTopRepository =
        YaniAnimeTopRepository(api, topStore, settingsStore)
}
