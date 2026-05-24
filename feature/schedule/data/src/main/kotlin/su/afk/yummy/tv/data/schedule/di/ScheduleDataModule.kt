package su.afk.yummy.tv.data.schedule.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.ktor.client.HttpClient
import kotlinx.serialization.json.Json
import su.afk.yummy.tv.core.storage.cache.CacheStore
import su.afk.yummy.tv.data.schedule.YaniScheduleRepository
import su.afk.yummy.tv.domain.schedule.AnimeScheduleRepository
import su.afk.yummy.tv.domain.schedule.GetAnimeScheduleUseCase
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ScheduleDataModule {
    @Provides
    @Singleton
    fun provideScheduleRepository(client: HttpClient, cache: CacheStore, json: Json): AnimeScheduleRepository =
        YaniScheduleRepository(client, cache, json)

    @Provides
    fun provideGetAnimeScheduleUseCase(repository: AnimeScheduleRepository) = GetAnimeScheduleUseCase(repository)
}
