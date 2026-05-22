package su.afk.yummy.tv.data.home.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.ktor.client.HttpClient
import kotlinx.serialization.json.Json
import su.afk.yummy.tv.core.error.StringProvider
import su.afk.yummy.tv.core.storage.cache.CacheStore
import su.afk.yummy.tv.data.home.YaniHomeFeedRepository
import su.afk.yummy.tv.domain.home.GetHomeFeedUseCase
import su.afk.yummy.tv.domain.home.HomeFeedRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object HomeDataModule {

    @Provides
    @Singleton
    fun provideHomeFeedRepository(
        client: HttpClient,
        cache: CacheStore,
        json: Json,
        stringProvider: StringProvider,
    ): HomeFeedRepository =
        YaniHomeFeedRepository(client, cache, json, stringProvider)

    @Provides
    fun provideGetHomeFeedUseCase(repo: HomeFeedRepository) = GetHomeFeedUseCase(repo)
}
