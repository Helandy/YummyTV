package su.afk.yummy.tv.data.home.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.ktor.client.HttpClient
import kotlinx.serialization.json.Json
import su.afk.yummy.tv.core.error.StringProvider
import su.afk.yummy.tv.core.storage.cache.CacheStore
import su.afk.yummy.tv.data.home.network.YaniHomeApi
import su.afk.yummy.tv.data.home.repository.YaniHomeFeedRepository
import su.afk.yummy.tv.domain.home.repository.HomeFeedRepository
import su.afk.yummy.tv.domain.home.usecase.GetHomeFeedUseCase
import su.afk.yummy.tv.domain.home.usecase.RefreshHomeFeedUseCase
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object HomeDataModule {

    @Provides
    @Singleton
    fun provideYaniHomeApi(client: HttpClient): YaniHomeApi = YaniHomeApi(client)

    @Provides
    @Singleton
    fun provideHomeFeedRepository(
        api: YaniHomeApi,
        cache: CacheStore,
        json: Json,
        stringProvider: StringProvider,
    ): HomeFeedRepository =
        YaniHomeFeedRepository(api, cache, json, stringProvider)

    @Provides
    fun provideGetHomeFeedUseCase(repo: HomeFeedRepository) = GetHomeFeedUseCase(repo)

    @Provides
    fun provideRefreshHomeFeedUseCase(repo: HomeFeedRepository) = RefreshHomeFeedUseCase(repo)
}
