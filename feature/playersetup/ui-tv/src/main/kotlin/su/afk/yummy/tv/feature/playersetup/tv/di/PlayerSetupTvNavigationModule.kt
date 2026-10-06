package su.afk.yummy.tv.feature.playersetup.tv.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import su.afk.yummy.tv.feature.playersetup.navigator.ITvPlayerSetupEntry
import su.afk.yummy.tv.feature.playersetup.tv.navigator.PlayerSetupTvNavRegistrar

@Module
@InstallIn(SingletonComponent::class)
interface PlayerSetupTvNavigationModule {

    @Binds
    fun bindPlayerSetupTvNavRegistrar(impl: PlayerSetupTvNavRegistrar): ITvPlayerSetupEntry
}
