package su.afk.yummy.tv.source.sameband.data.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import su.afk.yummy.tv.domain.anime.repository.ExtraVideoRepository
import su.afk.yummy.tv.domain.player.repository.ExtraStreamRepository
import su.afk.yummy.tv.source.sameband.data.repository.SamebandRepositoryImpl
import su.afk.yummy.tv.source.sameband.data.source.AndroidSamebandMessages
import su.afk.yummy.tv.source.sameband.data.source.SamebandExtraStreamRepository
import su.afk.yummy.tv.source.sameband.data.source.SamebandExtraVideoRepository
import su.afk.yummy.tv.source.sameband.data.source.SamebandMessages
import su.afk.yummy.tv.source.sameband.domain.repository.SamebandRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface SamebandModule {

    @Binds
    @Singleton
    fun bindSamebandRepository(impl: SamebandRepositoryImpl): SamebandRepository

    @Binds
    fun bindMessages(impl: AndroidSamebandMessages): SamebandMessages

    @Binds
    @IntoSet
    fun bindExtraVideoRepository(impl: SamebandExtraVideoRepository): ExtraVideoRepository

    @Binds
    @IntoSet
    fun bindExtraStreamRepository(impl: SamebandExtraStreamRepository): ExtraStreamRepository
}
