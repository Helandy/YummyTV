package su.afk.yummy.tv.source.aksor.data.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import su.afk.yummy.tv.feature.player.extractor.PlayerStreamExtractor
import su.afk.yummy.tv.source.aksor.data.extractor.AksorExtractor

@Module
@InstallIn(SingletonComponent::class)
internal interface AksorModule {

    @Binds
    @IntoSet
    fun bindAksorExtractor(extractor: AksorExtractor): PlayerStreamExtractor
}
