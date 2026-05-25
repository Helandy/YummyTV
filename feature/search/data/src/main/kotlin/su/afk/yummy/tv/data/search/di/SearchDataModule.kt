package su.afk.yummy.tv.data.search.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.ktor.client.HttpClient
import su.afk.yummy.tv.data.search.network.YaniSearchApi
import su.afk.yummy.tv.data.search.repository.YaniSearchRepository
import su.afk.yummy.tv.domain.search.GetSearchFilterOptionsUseCase
import su.afk.yummy.tv.domain.search.SearchRepository
import su.afk.yummy.tv.domain.search.SearchUseCase
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SearchDataModule {

    @Provides
    @Singleton
    fun provideYaniSearchApi(client: HttpClient): YaniSearchApi = YaniSearchApi(client)

    @Provides
    @Singleton
    fun provideSearchRepository(api: YaniSearchApi): SearchRepository = YaniSearchRepository(api)

    @Provides
    fun provideSearchUseCase(repo: SearchRepository) = SearchUseCase(repo)

    @Provides
    fun provideGetSearchFilterOptionsUseCase(repo: SearchRepository) = GetSearchFilterOptionsUseCase(repo)
}
