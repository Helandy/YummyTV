package su.afk.yummy.tv.feature.player.tv.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import su.afk.yummy.tv.feature.player.ITvPlayerEntry
import su.afk.yummy.tv.feature.player.tv.navigator.PlayerNavRegistrar

@Module
@InstallIn(SingletonComponent::class)
interface PlayerTvNavigationModule {

    @Binds
    fun bindPlayerNavRegistrar(impl: PlayerNavRegistrar): ITvPlayerEntry
}
