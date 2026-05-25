package su.afk.yummy.tv.data.collection.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.ktor.client.HttpClient
import kotlinx.serialization.json.Json
import su.afk.yummy.tv.core.storage.cache.CacheStore
import su.afk.yummy.tv.data.collection.network.YaniCollectionApi
import su.afk.yummy.tv.data.collection.repository.YaniCollectionDetailRepository
import su.afk.yummy.tv.domain.collection.CollectionRepository
import su.afk.yummy.tv.domain.collection.GetCollectionUseCase
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object CollectionDataModule {

    @Provides
    @Singleton
    fun provideYaniCollectionApi(client: HttpClient): YaniCollectionApi = YaniCollectionApi(client)

    @Provides
    @Singleton
    fun provideCollectionRepository(api: YaniCollectionApi, cache: CacheStore, json: Json): CollectionRepository =
        YaniCollectionDetailRepository(api, cache, json)

    @Provides
    fun provideGetCollectionUseCase(repo: CollectionRepository) = GetCollectionUseCase(repo)
}
