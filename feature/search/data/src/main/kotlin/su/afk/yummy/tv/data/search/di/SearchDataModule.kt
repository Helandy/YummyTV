package su.afk.yummy.tv.data.search.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.ktor.client.HttpClient
import kotlinx.serialization.json.Json
import su.afk.yummy.tv.core.preferences.settings.SettingsStore
import su.afk.yummy.tv.core.storage.cache.CacheStore
import su.afk.yummy.tv.core.storage.search.SearchStorageStore
import su.afk.yummy.tv.data.search.network.YaniSearchApi
import su.afk.yummy.tv.data.search.repository.YaniSearchRepository
import su.afk.yummy.tv.domain.search.repository.SearchRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SearchDataModule {

    @Provides
    @Singleton
    fun provideYaniSearchApi(client: HttpClient): YaniSearchApi = YaniSearchApi(client)

    @Provides
    @Singleton
    fun provideSearchRepository(
        api: YaniSearchApi,
        cache: CacheStore,
        searchStorage: SearchStorageStore,
        json: Json,
        settingsStore: SettingsStore,
    ): SearchRepository =
        YaniSearchRepository(api, cache, searchStorage, json, settingsStore)
}
