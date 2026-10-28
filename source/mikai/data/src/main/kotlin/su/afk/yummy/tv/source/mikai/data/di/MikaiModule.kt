package su.afk.yummy.tv.source.mikai.data.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import su.afk.yummy.tv.domain.anime.repository.ExtraVideoRepository
import su.afk.yummy.tv.domain.player.repository.ExtraStreamRepository
import su.afk.yummy.tv.source.mikai.data.source.AndroidMikaiMessages
import su.afk.yummy.tv.source.mikai.data.source.MikaiExtraStreamRepository
import su.afk.yummy.tv.source.mikai.data.source.MikaiExtraVideoRepository
import su.afk.yummy.tv.source.mikai.data.source.MikaiMessages

@Module
@InstallIn(SingletonComponent::class)
internal interface MikaiModule {

    @Binds
    fun bindMessages(impl: AndroidMikaiMessages): MikaiMessages

    @Binds
    @IntoSet
    fun bindExtraVideoRepository(impl: MikaiExtraVideoRepository): ExtraVideoRepository

    @Binds
    @IntoSet
    fun bindExtraStreamRepository(impl: MikaiExtraStreamRepository): ExtraStreamRepository
}
