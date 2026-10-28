package su.afk.yummy.tv.source.anidub.data.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import su.afk.yummy.tv.domain.anime.repository.ExtraVideoRepository
import su.afk.yummy.tv.domain.player.repository.ExtraStreamRepository
import su.afk.yummy.tv.source.anidub.data.repository.AniDubRepositoryImpl
import su.afk.yummy.tv.source.anidub.data.source.AniDubExtraStreamRepository
import su.afk.yummy.tv.source.anidub.data.source.AniDubExtraVideoRepository
import su.afk.yummy.tv.source.anidub.data.source.AniDubMessages
import su.afk.yummy.tv.source.anidub.data.source.AndroidAniDubMessages
import su.afk.yummy.tv.source.anidub.domain.repository.AniDubRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface AniDubModule {

    @Binds
    @Singleton
    fun bindAniDubRepository(impl: AniDubRepositoryImpl): AniDubRepository

    @Binds
    fun bindMessages(impl: AndroidAniDubMessages): AniDubMessages

    @Binds
    @IntoSet
    fun bindExtraVideoRepository(impl: AniDubExtraVideoRepository): ExtraVideoRepository

    @Binds
    @IntoSet
    fun bindExtraStreamRepository(impl: AniDubExtraStreamRepository): ExtraStreamRepository
}
