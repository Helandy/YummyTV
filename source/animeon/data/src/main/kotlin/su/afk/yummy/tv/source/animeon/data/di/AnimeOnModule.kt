package su.afk.yummy.tv.source.animeon.data.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import su.afk.yummy.tv.domain.anime.repository.ExtraVideoRepository
import su.afk.yummy.tv.domain.player.repository.ExtraStreamRepository
import su.afk.yummy.tv.source.animeon.data.repository.AnimeOnRepositoryImpl
import su.afk.yummy.tv.source.animeon.data.source.AndroidAnimeOnMessages
import su.afk.yummy.tv.source.animeon.data.source.AnimeOnExtraStreamRepository
import su.afk.yummy.tv.source.animeon.data.source.AnimeOnExtraVideoRepository
import su.afk.yummy.tv.source.animeon.data.source.AnimeOnMessages
import su.afk.yummy.tv.source.animeon.domain.repository.AnimeOnRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface AnimeOnModule {

    @Binds
    @Singleton
    fun bindAnimeOnRepository(impl: AnimeOnRepositoryImpl): AnimeOnRepository

    @Binds
    fun bindMessages(impl: AndroidAnimeOnMessages): AnimeOnMessages

    @Binds
    @IntoSet
    fun bindExtraVideoRepository(impl: AnimeOnExtraVideoRepository): ExtraVideoRepository

    @Binds
    @IntoSet
    fun bindExtraStreamRepository(impl: AnimeOnExtraStreamRepository): ExtraStreamRepository
}
