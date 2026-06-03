package su.afk.yummy.tv.feature.player.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import su.afk.yummy.tv.core.navigation.NavRegistrar
import su.afk.yummy.tv.feature.player.IPlayerNavigator
import su.afk.yummy.tv.feature.player.navigator.PlayerNavRegistrar
import su.afk.yummy.tv.feature.player.navigator.PlayerNavigator

@Module
@InstallIn(SingletonComponent::class)
interface PlayerNavModule {
    @Binds
    @IntoSet
    fun bindPlayerNavRegistrar(impl: PlayerNavRegistrar): NavRegistrar

    @Binds
    fun bindPlayerNavigator(impl: PlayerNavigator): IPlayerNavigator
}
