package su.afk.yummy.tv.data.collection.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.ktor.client.HttpClient
import kotlinx.serialization.json.Json
import su.afk.yummy.tv.core.storage.cache.CacheStore
import su.afk.yummy.tv.data.collection.YaniCollectionDetailRepository
import su.afk.yummy.tv.domain.collection.CollectionRepository
import su.afk.yummy.tv.domain.collection.GetCollectionUseCase
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object CollectionDataModule {

    @Provides
    @Singleton
    fun provideCollectionRepository(client: HttpClient, cache: CacheStore, json: Json): CollectionRepository =
        YaniCollectionDetailRepository(client, cache, json)

    @Provides
    fun provideGetCollectionUseCase(repo: CollectionRepository) = GetCollectionUseCase(repo)
}
