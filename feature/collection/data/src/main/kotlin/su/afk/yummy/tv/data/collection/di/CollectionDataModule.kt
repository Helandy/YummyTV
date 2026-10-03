package su.afk.yummy.tv.data.collection.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import su.afk.yummy.tv.data.collection.repository.DefaultCollectionMutationRepository
import su.afk.yummy.tv.core.network.yani.YaniHttpClientProvider
import su.afk.yummy.tv.core.preferences.settings.YaniAccountSettingsStore
import su.afk.yummy.tv.core.storage.account.AccountStorage
import su.afk.yummy.tv.core.storage.collection.CollectionStorage
import su.afk.yummy.tv.data.collection.network.YaniCollectionApi
import su.afk.yummy.tv.data.collection.repository.YaniCollectionDetailRepository
import su.afk.yummy.tv.domain.collection.repository.CollectionMutationRepository
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
        collectionStorage: CollectionStorage,
        accountStorage: AccountStorage,
        settingsStore: YaniAccountSettingsStore,
    ): CollectionRepository =
        YaniCollectionDetailRepository(api, collectionStorage, accountStorage, settingsStore)

    @Provides
    @Singleton
    fun provideCollectionMutationRepository(
        impl: DefaultCollectionMutationRepository,
    ): CollectionMutationRepository = impl
}
