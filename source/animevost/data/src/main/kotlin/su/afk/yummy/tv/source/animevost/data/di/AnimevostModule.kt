package su.afk.yummy.tv.source.animevost.data.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import su.afk.yummy.tv.domain.anime.repository.ExtraVideoRepository
import su.afk.yummy.tv.domain.player.repository.ExtraStreamRepository
import su.afk.yummy.tv.source.animevost.data.repository.AnimevostRepositoryImpl
import su.afk.yummy.tv.source.animevost.data.source.AndroidAnimevostMessages
import su.afk.yummy.tv.source.animevost.data.source.AnimevostExtraStreamRepository
import su.afk.yummy.tv.source.animevost.data.source.AnimevostExtraVideoRepository
import su.afk.yummy.tv.source.animevost.data.source.AnimevostMessages
import su.afk.yummy.tv.source.animevost.domain.repository.AnimevostRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface AnimevostModule {

    @Binds
    @Singleton
    fun bindAnimevostRepository(impl: AnimevostRepositoryImpl): AnimevostRepository

    @Binds
    fun bindMessages(impl: AndroidAnimevostMessages): AnimevostMessages

    @Binds
    @IntoSet
    fun bindExtraVideoRepository(impl: AnimevostExtraVideoRepository): ExtraVideoRepository

    @Binds
    @IntoSet
    fun bindExtraStreamRepository(impl: AnimevostExtraStreamRepository): ExtraStreamRepository
}
