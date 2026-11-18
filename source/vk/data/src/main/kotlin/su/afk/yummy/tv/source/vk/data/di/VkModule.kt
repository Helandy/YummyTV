package su.afk.yummy.tv.source.vk.data.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import su.afk.yummy.tv.feature.player.extractor.PlayerStreamExtractor
import su.afk.yummy.tv.source.vk.data.extractor.VkExtractor

@Module
@InstallIn(SingletonComponent::class)
internal interface VkModule {

    @Binds
    @IntoSet
    fun bindVkExtractor(extractor: VkExtractor): PlayerStreamExtractor
}
