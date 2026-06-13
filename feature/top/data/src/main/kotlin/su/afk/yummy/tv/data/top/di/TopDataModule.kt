package su.afk.yummy.tv.data.top.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.ktor.client.HttpClient
import kotlinx.serialization.json.Json
import su.afk.yummy.tv.core.preferences.settings.SettingsStore
import su.afk.yummy.tv.core.storage.cache.CacheStore
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
    fun provideYaniAnimeTopApi(client: HttpClient): YaniAnimeTopApi = YaniAnimeTopApi(client)

    @Provides
    @Singleton
    fun provideAnimeTopRepository(
        api: YaniAnimeTopApi,
        cache: CacheStore,
        topStore: AnimeTopStore,
        json: Json,
        settingsStore: SettingsStore,
    ): AnimeTopRepository =
        YaniAnimeTopRepository(api, cache, topStore, json, settingsStore)
}
