package su.afk.yummy.tv.source.sibnet.data.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import su.afk.yummy.tv.feature.player.extractor.PlayerStreamExtractor
import su.afk.yummy.tv.source.sibnet.data.extractor.SibnetExtractor

@Module
@InstallIn(SingletonComponent::class)
internal interface SibnetModule {

    @Binds
    @IntoSet
    fun bindSibnetExtractor(extractor: SibnetExtractor): PlayerStreamExtractor
}
