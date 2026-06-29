package su.afk.yummy.tv.data.player.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import su.afk.yummy.tv.data.player.extractor.AksorExtractor
import su.afk.yummy.tv.data.player.extractor.AllohaExtractor
import su.afk.yummy.tv.data.player.extractor.CvhExtractor
import su.afk.yummy.tv.data.player.extractor.KodikExtractor
import su.afk.yummy.tv.data.player.extractor.PlayerHttpClient
import su.afk.yummy.tv.data.player.extractor.PlayerStreamExtractor
import su.afk.yummy.tv.data.player.extractor.RutubeExtractor
import su.afk.yummy.tv.data.player.extractor.UrlConnectionPlayerHttpClient
import su.afk.yummy.tv.data.player.extractor.VkExtractor
import su.afk.yummy.tv.data.player.extractor.ZedfilmExtractor
import su.afk.yummy.tv.data.player.repository.DefaultPlayerSourceRepository
import su.afk.yummy.tv.data.player.repository.DefaultPlayerStreamRepository
import su.afk.yummy.tv.domain.player.repository.PlayerSourceRepository
import su.afk.yummy.tv.domain.player.repository.PlayerStreamRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object PlayerDataModule {

    @Provides
    @Singleton
    internal fun providePlayerHttpClient(
        client: UrlConnectionPlayerHttpClient,
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
    internal fun provideZedfilmExtractor(extractor: ZedfilmExtractor): PlayerStreamExtractor =
        extractor
}
