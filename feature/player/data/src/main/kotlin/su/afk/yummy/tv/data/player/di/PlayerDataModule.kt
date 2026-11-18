package su.afk.yummy.tv.data.player.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import javax.inject.Singleton
import su.afk.yummy.tv.data.player.extractor.extra.ExtraSourceExtractor
import su.afk.yummy.tv.data.player.repository.DefaultAllohaTrackPreferenceRepository
import su.afk.yummy.tv.data.player.repository.DefaultPlayerSourceRepository
import su.afk.yummy.tv.data.player.repository.DefaultPlayerStreamRepository
import su.afk.yummy.tv.data.player.repository.DefaultWatchProgressRepository
import su.afk.yummy.tv.domain.player.repository.AllohaTrackPreferenceRepository
import su.afk.yummy.tv.domain.player.repository.PlayerSourceRepository
import su.afk.yummy.tv.domain.player.repository.PlayerStreamRepository
import su.afk.yummy.tv.domain.player.repository.WatchProgressRepository
import su.afk.yummy.tv.feature.player.extractor.PlayerStreamExtractor

@Module
@InstallIn(SingletonComponent::class)
object PlayerDataModule {

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
    internal fun provideExtraSourceExtractor(extractor: ExtraSourceExtractor): PlayerStreamExtractor =
        extractor
}
