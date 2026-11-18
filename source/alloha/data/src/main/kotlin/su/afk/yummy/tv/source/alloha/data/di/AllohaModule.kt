package su.afk.yummy.tv.source.alloha.data.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import su.afk.yummy.tv.feature.player.extractor.PlayerStreamExtractor
import su.afk.yummy.tv.source.alloha.data.extractor.AllohaExtractor

@Module
@InstallIn(SingletonComponent::class)
internal interface AllohaModule {

    @Binds
    @IntoSet
    fun bindAllohaExtractor(extractor: AllohaExtractor): PlayerStreamExtractor
}
