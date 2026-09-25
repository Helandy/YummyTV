package su.afk.yummy.tv.feature.home.mobile.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import su.afk.yummy.tv.feature.home.IMobileHomeEntry
import su.afk.yummy.tv.feature.home.mobile.navigator.HomeNavRegistrar

@Module
@InstallIn(SingletonComponent::class)
interface HomeMobileNavigationModule {

    @Binds
    fun bindHomeNavRegistrar(impl: HomeNavRegistrar): IMobileHomeEntry
}
