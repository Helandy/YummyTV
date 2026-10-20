package su.afk.yummy.tv.source.anistar.data.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import su.afk.yummy.tv.domain.anime.repository.ExtraVideoRepository
import su.afk.yummy.tv.domain.player.repository.ExtraStreamRepository
import su.afk.yummy.tv.source.anistar.data.repository.AnistarRepositoryImpl
import su.afk.yummy.tv.source.anistar.data.source.AndroidAnistarMessages
import su.afk.yummy.tv.source.anistar.data.source.AnistarExtraStreamRepository
import su.afk.yummy.tv.source.anistar.data.source.AnistarExtraVideoRepository
import su.afk.yummy.tv.source.anistar.data.source.AnistarMessages
import su.afk.yummy.tv.source.anistar.domain.repository.AnistarRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface AnistarModule {

    @Binds
    @Singleton
    fun bindAnistarRepository(impl: AnistarRepositoryImpl): AnistarRepository

    @Binds
    fun bindMessages(impl: AndroidAnistarMessages): AnistarMessages

    @Binds
    @IntoSet
    fun bindExtraVideoRepository(impl: AnistarExtraVideoRepository): ExtraVideoRepository

    @Binds
    @IntoSet
    fun bindExtraStreamRepository(impl: AnistarExtraStreamRepository): ExtraStreamRepository
}
