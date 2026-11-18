package su.afk.yummy.tv.source.zedfilm.data.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import su.afk.yummy.tv.feature.player.extractor.PlayerStreamExtractor
import su.afk.yummy.tv.source.zedfilm.data.extractor.ZedfilmExtractor

@Module
@InstallIn(SingletonComponent::class)
internal interface ZedfilmModule {

    @Binds
    @IntoSet
    fun bindZedfilmExtractor(extractor: ZedfilmExtractor): PlayerStreamExtractor
}
