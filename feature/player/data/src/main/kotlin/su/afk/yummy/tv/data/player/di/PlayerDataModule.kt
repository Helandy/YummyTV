package su.afk.yummy.tv.data.player.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import su.afk.yummy.tv.data.player.extractor.PlayerStreamExtractor
import su.afk.yummy.tv.data.player.extractor.aksor.AksorExtractor
import su.afk.yummy.tv.data.player.extractor.alloha.AllohaExtractor
import su.afk.yummy.tv.data.player.extractor.cvh.CvhExtractor
import su.afk.yummy.tv.data.player.extractor.kodik.KodikExtractor
import su.afk.yummy.tv.data.player.extractor.rutube.RutubeExtractor
import su.afk.yummy.tv.data.player.extractor.sibnet.SibnetExtractor
import su.afk.yummy.tv.data.player.extractor.vk.VkExtractor
import su.afk.yummy.tv.data.player.extractor.zedfilm.ZedfilmExtractor
import su.afk.yummy.tv.data.player.network.KtorPlayerHttpClient
import su.afk.yummy.tv.data.player.network.PlayerHttpClient
import su.afk.yummy.tv.data.player.repository.DefaultAllohaTrackPreferenceRepository
import su.afk.yummy.tv.data.player.repository.DefaultPlayerSourceRepository
import su.afk.yummy.tv.data.player.repository.DefaultPlayerStreamRepository
import su.afk.yummy.tv.data.player.repository.DefaultWatchProgressRepository
import su.afk.yummy.tv.data.player.session.DefaultAllohaPlaybackSessionManager
import su.afk.yummy.tv.domain.player.repository.AllohaTrackPreferenceRepository
import su.afk.yummy.tv.domain.player.repository.PlayerSourceRepository
import su.afk.yummy.tv.domain.player.repository.PlayerStreamRepository
import su.afk.yummy.tv.domain.player.repository.WatchProgressRepository
import su.afk.yummy.tv.domain.player.repository.AllohaPlaybackSessionRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object PlayerDataModule {
    @Provides
    @Singleton
    internal fun provideAllohaPlaybackSessionManager(
        manager: DefaultAllohaPlaybackSessionManager,
    ): AllohaPlaybackSessionRepository = manager


    @Provides
    @Singleton
    internal fun providePlayerHttpClient(
        client: KtorPlayerHttpClient,
    ): PlayerHttpClient = client

    @Provides
    @Singleton
    internal fun providePlayerStreamRepository(
        repository: DefaultPlayerStreamRepository,
    ): PlayerStreamRepository = repository

    @Provides
    @Singleton
    internal fun providePlayerSourceRepository(
        repository: DefaultPlayerSourceRepository,
    ): PlayerSourceRepository = repository

    @Provides
    @Singleton
    internal fun provideWatchProgressRepository(
        repository: DefaultWatchProgressRepository,
    ): WatchProgressRepository = repository

    @Provides
    @Singleton
    internal fun provideAllohaTrackPreferenceRepository(
        repository: DefaultAllohaTrackPreferenceRepository,
    ): AllohaTrackPreferenceRepository = repository

    @Provides
    @IntoSet
    internal fun provideAllohaExtractor(extractor: AllohaExtractor): PlayerStreamExtractor =
        extractor

    @Provides
    @IntoSet
    internal fun provideKodikExtractor(extractor: KodikExtractor): PlayerStreamExtractor = extractor

    @Provides
    @IntoSet
    internal fun provideAksorExtractor(extractor: AksorExtractor): PlayerStreamExtractor = extractor

    @Provides
    @IntoSet
    internal fun provideCvhExtractor(extractor: CvhExtractor): PlayerStreamExtractor = extractor

    @Provides
    @IntoSet
    internal fun provideVkExtractor(extractor: VkExtractor): PlayerStreamExtractor = extractor

    @Provides
    @IntoSet
    internal fun provideRutubeExtractor(extractor: RutubeExtractor): PlayerStreamExtractor =
        extractor

    @Provides
    @IntoSet
    internal fun provideSibnetExtractor(extractor: SibnetExtractor): PlayerStreamExtractor =
        extractor

    @Provides
    @IntoSet
    internal fun provideZedfilmExtractor(extractor: ZedfilmExtractor): PlayerStreamExtractor =
        extractor
}
