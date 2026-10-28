package su.afk.yummy.tv.source.animebesst.data.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import su.afk.yummy.tv.domain.anime.repository.ExtraVideoRepository
import su.afk.yummy.tv.domain.player.repository.ExtraStreamRepository
import su.afk.yummy.tv.source.animebesst.data.repository.AnimebesstRepositoryImpl
import su.afk.yummy.tv.source.animebesst.data.source.AndroidAnimebesstMessages
import su.afk.yummy.tv.source.animebesst.data.source.AnimebesstExtraStreamRepository
import su.afk.yummy.tv.source.animebesst.data.source.AnimebesstExtraVideoRepository
import su.afk.yummy.tv.source.animebesst.data.source.AnimebesstMessages
import su.afk.yummy.tv.source.animebesst.domain.repository.AnimebesstRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface AnimebesstModule {

    @Binds
    @Singleton
    fun bindAnimebesstRepository(impl: AnimebesstRepositoryImpl): AnimebesstRepository

    @Binds
    fun bindMessages(impl: AndroidAnimebesstMessages): AnimebesstMessages

    @Binds
    @IntoSet
    fun bindExtraVideoRepository(impl: AnimebesstExtraVideoRepository): ExtraVideoRepository

    @Binds
    @IntoSet
    fun bindExtraStreamRepository(impl: AnimebesstExtraStreamRepository): ExtraStreamRepository
}
