package su.afk.yummy.tv.data.details.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.ktor.client.HttpClient
import su.afk.yummy.tv.core.preferences.settings.SettingsStore
import su.afk.yummy.tv.core.storage.account.AccountStorageStore
import su.afk.yummy.tv.core.storage.anime.AnimeStorageStore
import su.afk.yummy.tv.core.storage.watchprogress.WatchProgressStore
import su.afk.yummy.tv.data.details.network.YaniAnimeApi
import su.afk.yummy.tv.data.details.repository.YaniAnimeRepository
import su.afk.yummy.tv.domain.anime.repository.AnimeRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DetailsDataModule {

    @Provides
    @Singleton
    fun provideYaniAnimeApi(client: HttpClient): YaniAnimeApi = YaniAnimeApi(client)

    @Provides
    @Singleton
    fun provideAnimeRepository(
        api: YaniAnimeApi,
        animeStorage: AnimeStorageStore,
        accountStorage: AccountStorageStore,
        settingsStore: SettingsStore,
        watchProgressStore: WatchProgressStore,
    ): AnimeRepository =
        YaniAnimeRepository(api, animeStorage, accountStorage, settingsStore, watchProgressStore)
}
