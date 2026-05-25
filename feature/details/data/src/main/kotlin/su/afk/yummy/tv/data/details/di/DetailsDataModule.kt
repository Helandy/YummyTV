package su.afk.yummy.tv.data.details.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.ktor.client.HttpClient
import kotlinx.serialization.json.Json
import su.afk.yummy.tv.core.storage.cache.CacheStore
import su.afk.yummy.tv.data.details.network.YaniAnimeApi
import su.afk.yummy.tv.data.details.repository.YaniAnimePreviewRepository
import su.afk.yummy.tv.data.details.repository.YaniAnimeRepository
import su.afk.yummy.tv.domain.anime.AnimePreviewRepository
import su.afk.yummy.tv.domain.anime.AnimeRepository
import su.afk.yummy.tv.domain.anime.GetAnimeDetailsUseCase
import su.afk.yummy.tv.domain.anime.GetAnimePreviewUseCase
import su.afk.yummy.tv.domain.anime.GetAnimeRecommendationsUseCase
import su.afk.yummy.tv.domain.anime.GetAnimeTrailersUseCase
import su.afk.yummy.tv.domain.anime.GetAnimeVideosUseCase
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DetailsDataModule {

    @Provides
    @Singleton
    fun provideYaniAnimeApi(client: HttpClient): YaniAnimeApi = YaniAnimeApi(client)

    @Provides
    @Singleton
    fun provideAnimeRepository(api: YaniAnimeApi, cache: CacheStore, json: Json): AnimeRepository =
        YaniAnimeRepository(api, cache, json)

    @Provides
    @Singleton
    fun provideAnimePreviewRepository(api: YaniAnimeApi, cache: CacheStore, json: Json): AnimePreviewRepository =
        YaniAnimePreviewRepository(api, cache, json)

    @Provides
    fun provideGetAnimeDetailsUseCase(repo: AnimeRepository) = GetAnimeDetailsUseCase(repo)

    @Provides
    fun provideGetAnimeVideosUseCase(repo: AnimeRepository) = GetAnimeVideosUseCase(repo)

    @Provides
    fun provideGetAnimeTrailersUseCase(repo: AnimeRepository) = GetAnimeTrailersUseCase(repo)

    @Provides
    fun provideGetAnimeRecommendationsUseCase(repo: AnimeRepository) = GetAnimeRecommendationsUseCase(repo)

    @Provides
    fun provideGetAnimePreviewUseCase(repo: AnimePreviewRepository) = GetAnimePreviewUseCase(repo)
}
