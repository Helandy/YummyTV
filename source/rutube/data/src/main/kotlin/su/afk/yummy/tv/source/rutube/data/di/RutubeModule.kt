package su.afk.yummy.tv.source.rutube.data.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import su.afk.yummy.tv.feature.player.extractor.PlayerStreamExtractor
import su.afk.yummy.tv.source.rutube.data.extractor.RutubeExtractor

@Module
@InstallIn(SingletonComponent::class)
internal interface RutubeModule {

    @Binds
    @IntoSet
    fun bindRutubeExtractor(extractor: RutubeExtractor): PlayerStreamExtractor
}
