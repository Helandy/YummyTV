package su.afk.yummy.tv.source.cvh.data.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import su.afk.yummy.tv.feature.player.extractor.PlayerStreamExtractor
import su.afk.yummy.tv.source.cvh.data.extractor.CvhExtractor

@Module
@InstallIn(SingletonComponent::class)
internal interface CvhModule {

    @Binds
    @IntoSet
    fun bindCvhExtractor(extractor: CvhExtractor): PlayerStreamExtractor
}
