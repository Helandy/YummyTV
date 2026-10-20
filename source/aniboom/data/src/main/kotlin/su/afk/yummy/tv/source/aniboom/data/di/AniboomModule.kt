package su.afk.yummy.tv.source.aniboom.data.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import su.afk.yummy.tv.domain.anime.repository.ExtraVideoRepository
import su.afk.yummy.tv.domain.player.repository.ExtraStreamRepository
import su.afk.yummy.tv.source.aniboom.data.repository.AniboomRepositoryImpl
import su.afk.yummy.tv.source.aniboom.data.source.AndroidAniboomMessages
import su.afk.yummy.tv.source.aniboom.data.source.AniboomExtraStreamRepository
import su.afk.yummy.tv.source.aniboom.data.source.AniboomExtraVideoRepository
import su.afk.yummy.tv.source.aniboom.data.source.AniboomMessages
import su.afk.yummy.tv.source.aniboom.domain.repository.AniboomRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface AniboomModule {

    @Binds
    @Singleton
    fun bindAniboomRepository(impl: AniboomRepositoryImpl): AniboomRepository

    @Binds
    fun bindMessages(impl: AndroidAniboomMessages): AniboomMessages

    @Binds
    @IntoSet
    fun bindExtraVideoRepository(impl: AniboomExtraVideoRepository): ExtraVideoRepository

    @Binds
    @IntoSet
    fun bindExtraStreamRepository(impl: AniboomExtraStreamRepository): ExtraStreamRepository
}
