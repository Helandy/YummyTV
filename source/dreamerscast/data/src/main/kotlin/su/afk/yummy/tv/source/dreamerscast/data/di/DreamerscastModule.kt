package su.afk.yummy.tv.source.dreamerscast.data.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import su.afk.yummy.tv.domain.anime.repository.ExtraVideoRepository
import su.afk.yummy.tv.domain.player.repository.ExtraStreamRepository
import su.afk.yummy.tv.source.dreamerscast.data.repository.DreamerscastRepositoryImpl
import su.afk.yummy.tv.source.dreamerscast.data.source.AndroidDreamerscastMessages
import su.afk.yummy.tv.source.dreamerscast.data.source.DreamerscastExtraStreamRepository
import su.afk.yummy.tv.source.dreamerscast.data.source.DreamerscastExtraVideoRepository
import su.afk.yummy.tv.source.dreamerscast.data.source.DreamerscastMessages
import su.afk.yummy.tv.source.dreamerscast.domain.repository.DreamerscastRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface DreamerscastModule {

    @Binds
    @Singleton
    fun bindDreamerscastRepository(impl: DreamerscastRepositoryImpl): DreamerscastRepository

    @Binds
    fun bindMessages(impl: AndroidDreamerscastMessages): DreamerscastMessages

    @Binds
    @IntoSet
    fun bindExtraVideoRepository(impl: DreamerscastExtraVideoRepository): ExtraVideoRepository

    @Binds
    @IntoSet
    fun bindExtraStreamRepository(impl: DreamerscastExtraStreamRepository): ExtraStreamRepository
}
