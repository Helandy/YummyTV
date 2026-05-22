package su.afk.yummy.tv.data.top100.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.ktor.client.HttpClient
import kotlinx.serialization.json.Json
import su.afk.yummy.tv.core.storage.cache.CacheStore
import su.afk.yummy.tv.data.top100.YaniAnimeTopRepository
import su.afk.yummy.tv.domain.top100.AnimeTopRepository
import su.afk.yummy.tv.domain.top100.GetAnimeTopUseCase
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object Top100DataModule {

    @Provides
    @Singleton
    fun provideAnimeTopRepository(client: HttpClient, cache: CacheStore, json: Json): AnimeTopRepository =
        YaniAnimeTopRepository(client, cache, json)

    @Provides
    fun provideGetAnimeTopUseCase(repo: AnimeTopRepository) = GetAnimeTopUseCase(repo)
}
