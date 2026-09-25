package su.afk.yummy.tv.feature.top.tv.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import su.afk.yummy.tv.feature.top.ITvTopEntry
import su.afk.yummy.tv.feature.top.tv.navigator.TopNavRegistrar

@Module
@InstallIn(SingletonComponent::class)
interface TopTvNavigationModule {

    @Binds
    fun bindTopNavRegistrar(impl: TopNavRegistrar): ITvTopEntry
}
