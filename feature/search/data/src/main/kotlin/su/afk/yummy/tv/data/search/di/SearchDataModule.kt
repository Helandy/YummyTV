package su.afk.yummy.tv.data.search.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.ktor.client.HttpClient
import su.afk.yummy.tv.data.search.YaniSearchRepository
import su.afk.yummy.tv.domain.search.GetSearchFilterOptionsUseCase
import su.afk.yummy.tv.domain.search.SearchRepository
import su.afk.yummy.tv.domain.search.SearchUseCase
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SearchDataModule {

    @Provides
    @Singleton
    fun provideSearchRepository(client: HttpClient): SearchRepository = YaniSearchRepository(client)

    @Provides
    fun provideSearchUseCase(
        repo: SearchRepository,
        @Named("hideRegionBlocked") hideRegionBlocked: Boolean,
    ) = SearchUseCase(repo, hideRegionBlocked)

    @Provides
    fun provideGetSearchFilterOptionsUseCase(repo: SearchRepository) = GetSearchFilterOptionsUseCase(repo)
}
