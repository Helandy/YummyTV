package su.afk.yummy.tv.data.collection.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import su.afk.yummy.tv.core.network.YaniHttpClientProvider
import su.afk.yummy.tv.core.preferences.settings.SettingsStore
import su.afk.yummy.tv.core.storage.collection.CollectionStorageStore
import su.afk.yummy.tv.data.collection.network.YaniCollectionApi
import su.afk.yummy.tv.data.collection.repository.YaniCollectionDetailRepository
import su.afk.yummy.tv.domain.collection.repository.CollectionRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object CollectionDataModule {

    @Provides
    @Singleton
    fun provideYaniCollectionApi(clientProvider: YaniHttpClientProvider): YaniCollectionApi =
        YaniCollectionApi(clientProvider)

    @Provides
    @Singleton
    fun provideCollectionRepository(
        api: YaniCollectionApi,
        collectionStorage: CollectionStorageStore,
        settingsStore: SettingsStore,
    ): CollectionRepository =
        YaniCollectionDetailRepository(api, collectionStorage, settingsStore)
}
