package su.afk.yummy.tv.feature.top100.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import su.afk.yummy.tv.core.navigation.NavRegistrar
import su.afk.yummy.tv.feature.top100.ITop100Navigator
import su.afk.yummy.tv.feature.top100.navigator.Top100NavRegistrar
import su.afk.yummy.tv.feature.top100.navigator.Top100Navigator

@Module
@InstallIn(SingletonComponent::class)
interface Top100NavModule {
    @Binds
    @IntoSet
    fun bindTop100NavRegistrar(impl: Top100NavRegistrar): NavRegistrar

    @Binds
    fun bindTop100Navigator(impl: Top100Navigator): ITop100Navigator
}
