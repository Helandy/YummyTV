package su.afk.yummy.tv.source.kodik.data.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import su.afk.yummy.tv.feature.player.extractor.PlayerStreamExtractor
import su.afk.yummy.tv.source.kodik.data.extractor.KodikExtractor

@Module
@InstallIn(SingletonComponent::class)
internal interface KodikModule {

    @Binds
    @IntoSet
    fun bindKodikExtractor(extractor: KodikExtractor): PlayerStreamExtractor
}
