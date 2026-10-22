package su.afk.yummy.tv.source.amedia.data.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import su.afk.yummy.tv.domain.anime.repository.ExtraVideoRepository
import su.afk.yummy.tv.domain.player.repository.ExtraStreamRepository
import su.afk.yummy.tv.source.amedia.data.repository.AmediaRepositoryImpl
import su.afk.yummy.tv.source.amedia.data.source.AmediaExtraStreamRepository
import su.afk.yummy.tv.source.amedia.data.source.AmediaExtraVideoRepository
import su.afk.yummy.tv.source.amedia.data.source.AmediaMessages
import su.afk.yummy.tv.source.amedia.data.source.AndroidAmediaMessages
import su.afk.yummy.tv.source.amedia.domain.repository.AmediaRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface AmediaModule {

    @Binds
    @Singleton
    fun bindAmediaRepository(impl: AmediaRepositoryImpl): AmediaRepository

    @Binds
    fun bindMessages(impl: AndroidAmediaMessages): AmediaMessages

    @Binds
    @IntoSet
    fun bindExtraVideoRepository(impl: AmediaExtraVideoRepository): ExtraVideoRepository

    @Binds
    @IntoSet
    fun bindExtraStreamRepository(impl: AmediaExtraStreamRepository): ExtraStreamRepository
}
