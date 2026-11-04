package su.afk.yummy.tv.source.studiomir.data.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import su.afk.yummy.tv.domain.anime.repository.ExtraVideoRepository
import su.afk.yummy.tv.domain.player.repository.ExtraStreamRepository
import su.afk.yummy.tv.source.studiomir.data.repository.StudioMirRepositoryImpl
import su.afk.yummy.tv.source.studiomir.data.source.StudioMirExtraStreamRepository
import su.afk.yummy.tv.source.studiomir.data.source.StudioMirExtraVideoRepository
import su.afk.yummy.tv.source.studiomir.data.source.StudioMirMessages
import su.afk.yummy.tv.source.studiomir.data.source.AndroidStudioMirMessages
import su.afk.yummy.tv.source.studiomir.domain.repository.StudioMirRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface StudioMirModule {

    @Binds
    @Singleton
    fun bindStudioMirRepository(impl: StudioMirRepositoryImpl): StudioMirRepository

    @Binds
    fun bindMessages(impl: AndroidStudioMirMessages): StudioMirMessages

    @Binds
    @IntoSet
    fun bindExtraVideoRepository(impl: StudioMirExtraVideoRepository): ExtraVideoRepository

    @Binds
    @IntoSet
    fun bindExtraStreamRepository(impl: StudioMirExtraStreamRepository): ExtraStreamRepository
}
