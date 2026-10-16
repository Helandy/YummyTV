package su.afk.yummy.tv.data.catalog.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import su.afk.yummy.tv.core.network.yani.YaniHttpClientProvider
import su.afk.yummy.tv.core.preferences.api.YaniAccountSettingsStore
import su.afk.yummy.tv.core.storage.search.SearchStorage
import su.afk.yummy.tv.data.catalog.network.YaniSearchApi
import su.afk.yummy.tv.data.catalog.repository.YaniSearchRepository
import su.afk.yummy.tv.domain.catalog.repository.SearchRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object CatalogDataModule {

    @Provides
    @Singleton
    fun provideYaniSearchApi(clientProvider: YaniHttpClientProvider): YaniSearchApi =
        YaniSearchApi(clientProvider)

    @Provides
    @Singleton
    fun provideSearchRepository(
        api: YaniSearchApi,
        searchStorage: SearchStorage,
        settingsStore: YaniAccountSettingsStore,
    ): SearchRepository =
        YaniSearchRepository(api, searchStorage, settingsStore)
}
