package su.afk.yummy.tv.source.ailiberty.data.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import su.afk.yummy.tv.domain.anime.repository.ExtraVideoRepository
import su.afk.yummy.tv.domain.player.repository.ExtraStreamRepository
import su.afk.yummy.tv.source.ailiberty.data.repository.AiLibertyRepositoryImpl
import su.afk.yummy.tv.source.ailiberty.data.source.AiLibertyExtraStreamRepository
import su.afk.yummy.tv.source.ailiberty.data.source.AiLibertyExtraVideoRepository
import su.afk.yummy.tv.source.ailiberty.data.source.AiLibertyMessages
import su.afk.yummy.tv.source.ailiberty.data.source.AndroidAiLibertyMessages
import su.afk.yummy.tv.source.ailiberty.domain.repository.AiLibertyRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface AiLibertyModule {

    @Binds
    @Singleton
    fun bindAiLibertyRepository(impl: AiLibertyRepositoryImpl): AiLibertyRepository

    @Binds
    fun bindMessages(impl: AndroidAiLibertyMessages): AiLibertyMessages

    @Binds
    @IntoSet
    fun bindExtraVideoRepository(impl: AiLibertyExtraVideoRepository): ExtraVideoRepository

    @Binds
    @IntoSet
    fun bindExtraStreamRepository(impl: AiLibertyExtraStreamRepository): ExtraStreamRepository
}
