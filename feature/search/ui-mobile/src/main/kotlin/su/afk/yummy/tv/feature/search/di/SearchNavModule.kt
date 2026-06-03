package su.afk.yummy.tv.feature.search.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import su.afk.yummy.tv.core.navigation.NavRegistrar
import su.afk.yummy.tv.feature.search.ISearchNavigator
import su.afk.yummy.tv.feature.search.navigator.SearchNavRegistrar
import su.afk.yummy.tv.feature.search.navigator.SearchNavigator

@Module
@InstallIn(SingletonComponent::class)
interface SearchNavModule {
    @Binds
    @IntoSet
    fun bindSearchNavRegistrar(impl: SearchNavRegistrar): NavRegistrar

    @Binds
    fun bindSearchNavigator(impl: SearchNavigator): ISearchNavigator
}
