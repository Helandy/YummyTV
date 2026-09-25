package su.afk.yummy.tv.feature.player.mobile.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import su.afk.yummy.tv.feature.player.IMobilePlayerEntry
import su.afk.yummy.tv.feature.player.mobile.navigator.PlayerNavRegistrar

@Module
@InstallIn(SingletonComponent::class)
interface PlayerMobileNavigationModule {

    @Binds
    fun bindPlayerNavRegistrar(impl: PlayerNavRegistrar): IMobilePlayerEntry
}
