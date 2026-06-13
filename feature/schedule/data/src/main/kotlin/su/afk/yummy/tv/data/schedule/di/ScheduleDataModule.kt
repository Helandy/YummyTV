package su.afk.yummy.tv.data.schedule.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.ktor.client.HttpClient
import su.afk.yummy.tv.core.preferences.settings.SettingsStore
import su.afk.yummy.tv.core.storage.schedule.AnimeScheduleStore
import su.afk.yummy.tv.data.schedule.network.YaniScheduleApi
import su.afk.yummy.tv.data.schedule.repository.YaniScheduleRepository
import su.afk.yummy.tv.domain.schedule.repository.AnimeScheduleRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ScheduleDataModule {
    @Provides
    @Singleton
    fun provideYaniScheduleApi(client: HttpClient): YaniScheduleApi = YaniScheduleApi(client)

    @Provides
    @Singleton
    fun provideScheduleRepository(
        api: YaniScheduleApi,
        scheduleStore: AnimeScheduleStore,
        settingsStore: SettingsStore,
    ): AnimeScheduleRepository =
        YaniScheduleRepository(api, scheduleStore, settingsStore)
}
