package su.afk.yummy.tv.feature.main.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import su.afk.yummy.tv.feature.main.TvMainGraph
import su.afk.yummy.tv.feature.main.api.ITvMainGraph
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
interface TvMainNavModule {

    @Binds
    @Singleton
    fun bindTvMainGraph(impl: TvMainGraph): ITvMainGraph
}
