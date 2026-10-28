package su.afk.yummy.tv.data.details.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import su.afk.yummy.tv.core.network.yani.YaniHttpClientProvider
import su.afk.yummy.tv.core.preferences.api.YaniAccountSettingsStore
import su.afk.yummy.tv.core.storage.account.AccountStorage
import su.afk.yummy.tv.core.storage.anime.AnimeStorage
import su.afk.yummy.tv.core.storage.document.DocumentCacheStorage
import su.afk.yummy.tv.core.storage.watchprogress.WatchProgressStorage
import su.afk.yummy.tv.data.details.network.YaniAnimeApi
import su.afk.yummy.tv.core.network.yummy.SiteApiConfig
import su.afk.yummy.tv.core.network.yummy.SiteRequestSigner
import su.afk.yummy.tv.core.utils.episode.EpisodeThumbnailResolver
import su.afk.yummy.tv.data.details.network.MalEpisodeFlagsApi
import su.afk.yummy.tv.data.details.thumbnail.DefaultEpisodeThumbnailResolver
import su.afk.yummy.tv.data.details.network.YummyEpisodesApi
import su.afk.yummy.tv.data.details.repository.YaniAnimeRepository
import su.afk.yummy.tv.domain.anime.repository.AnimeRepository
import su.afk.yummy.tv.domain.anime.repository.ExtraVideoRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DetailsDataModule {

    @Provides
    @Singleton
    fun provideYaniAnimeApi(clientProvider: YaniHttpClientProvider): YaniAnimeApi =
        YaniAnimeApi(clientProvider)

    @Provides
    @Singleton
    fun provideYummyEpisodesApi(
        clientProvider: YaniHttpClientProvider,
        siteApiConfig: SiteApiConfig,
        signers: Set<@JvmSuppressWildcards SiteRequestSigner>,
    ): YummyEpisodesApi =
        YummyEpisodesApi(clientProvider, siteApiConfig, signers)

    @Provides
    @Singleton
    fun provideEpisodeThumbnailResolver(impl: DefaultEpisodeThumbnailResolver): EpisodeThumbnailResolver = impl

    @Provides
    @Singleton
    fun provideAnimeRepository(
        api: YaniAnimeApi,
        episodesApi: YummyEpisodesApi,
        episodeFlagsApi: MalEpisodeFlagsApi,
        animeStorage: AnimeStorage,
        accountStorage: AccountStorage,
        settingsStore: YaniAccountSettingsStore,
        watchProgressStore: WatchProgressStorage,
        documentCache: DocumentCacheStorage,
        extraVideoRepositories: Set<@JvmSuppressWildcards ExtraVideoRepository>,
    ): AnimeRepository =
        YaniAnimeRepository(
            api,
            episodesApi,
            episodeFlagsApi,
            animeStorage,
            accountStorage,
            settingsStore,
            watchProgressStore,
            documentCache,
            extraVideoRepositories,
        )
}
