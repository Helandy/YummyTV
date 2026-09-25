package su.afk.yummy.tv.feature.top.mobile.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import su.afk.yummy.tv.feature.top.IMobileTopEntry
import su.afk.yummy.tv.feature.top.mobile.navigator.TopNavRegistrar

@Module
@InstallIn(SingletonComponent::class)
interface TopMobileNavigationModule {

    @Binds
    fun bindTopNavRegistrar(impl: TopNavRegistrar): IMobileTopEntry
}
