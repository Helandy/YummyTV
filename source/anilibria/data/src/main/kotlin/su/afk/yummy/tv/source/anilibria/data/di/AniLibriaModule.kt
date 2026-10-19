package su.afk.yummy.tv.source.anilibria.data.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import su.afk.yummy.tv.domain.anime.repository.ExtraVideoRepository
import su.afk.yummy.tv.domain.player.repository.ExtraStreamRepository
import su.afk.yummy.tv.source.anilibria.data.repository.AniLibriaRepositoryImpl
import su.afk.yummy.tv.source.anilibria.data.source.AndroidAniLibriaMessages
import su.afk.yummy.tv.source.anilibria.data.source.AniLibriaExtraStreamRepository
import su.afk.yummy.tv.source.anilibria.data.source.AniLibriaExtraVideoRepository
import su.afk.yummy.tv.source.anilibria.data.source.AniLibriaMessages
import su.afk.yummy.tv.source.anilibria.domain.repository.AniLibriaRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface AniLibriaModule {

    @Binds
    @Singleton
    fun bindAniLibriaRepository(impl: AniLibriaRepositoryImpl): AniLibriaRepository

    @Binds
    fun bindMessages(impl: AndroidAniLibriaMessages): AniLibriaMessages

    @Binds
    @IntoSet
    fun bindExtraVideoRepository(impl: AniLibriaExtraVideoRepository): ExtraVideoRepository

    @Binds
    @IntoSet
    fun bindExtraStreamRepository(impl: AniLibriaExtraStreamRepository): ExtraStreamRepository
}
