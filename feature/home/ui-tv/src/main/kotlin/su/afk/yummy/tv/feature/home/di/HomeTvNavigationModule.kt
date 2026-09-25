package su.afk.yummy.tv.feature.home.tv.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import su.afk.yummy.tv.feature.home.ITvHomeEntry
import su.afk.yummy.tv.feature.home.tv.navigator.HomeNavRegistrar

@Module
@InstallIn(SingletonComponent::class)
interface HomeTvNavigationModule {

    @Binds
    fun bindHomeNavRegistrar(impl: HomeNavRegistrar): ITvHomeEntry
}
