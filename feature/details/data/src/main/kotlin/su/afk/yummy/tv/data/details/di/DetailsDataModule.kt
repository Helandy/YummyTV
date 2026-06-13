package su.afk.yummy.tv.data.details.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.ktor.client.HttpClient
import kotlinx.serialization.json.Json
import su.afk.yummy.tv.core.preferences.settings.SettingsStore
import su.afk.yummy.tv.core.storage.anime.AnimeStorageStore
import su.afk.yummy.tv.core.storage.cache.CacheStore
import su.afk.yummy.tv.data.details.network.YaniAnimeApi
import su.afk.yummy.tv.data.details.repository.YaniAnimeRepository
import su.afk.yummy.tv.domain.anime.repository.AnimeRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DetailsDataModule {

    @Provides
    @Singleton
    fun provideYaniAnimeApi(client: HttpClient): YaniAnimeApi = YaniAnimeApi(client)

    @Provides
    @Singleton
    fun provideAnimeRepository(
        api: YaniAnimeApi,
        cache: CacheStore,
        animeStorage: AnimeStorageStore,
        json: Json,
        settingsStore: SettingsStore,
    ): AnimeRepository =
        YaniAnimeRepository(api, cache, animeStorage, json, settingsStore)
}
