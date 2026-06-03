package su.afk.yummy.tv.feature.home.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import su.afk.yummy.tv.core.navigation.NavRegistrar
import su.afk.yummy.tv.feature.home.IHomeNavigator
import su.afk.yummy.tv.feature.home.navigator.HomeNavRegistrar
import su.afk.yummy.tv.feature.home.navigator.HomeNavigator

@Module
@InstallIn(SingletonComponent::class)
interface HomeNavModule {
    @Binds
    @IntoSet
    fun bindHomeNavRegistrar(impl: HomeNavRegistrar): NavRegistrar

    @Binds
    fun bindHomeNavigator(impl: HomeNavigator): IHomeNavigator
}
