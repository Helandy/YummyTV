package su.afk.yummy.tv.external.tenrai.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import su.afk.yummy.tv.domain.anime.repository.EpisodeFlagsSource
import su.afk.yummy.tv.external.tenrai.source.TenraiEpisodeFlagsSource
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface TenraiModule {

    @Binds
    @IntoSet
    @Singleton
    fun bindEpisodeFlagsSource(impl: TenraiEpisodeFlagsSource): EpisodeFlagsSource
}
