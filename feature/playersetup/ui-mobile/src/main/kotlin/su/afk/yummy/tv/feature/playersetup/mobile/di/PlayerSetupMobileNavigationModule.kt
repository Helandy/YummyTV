package su.afk.yummy.tv.feature.playersetup.mobile.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import su.afk.yummy.tv.feature.playersetup.mobile.navigator.PlayerSetupMobileNavRegistrar
import su.afk.yummy.tv.feature.playersetup.navigator.IMobilePlayerSetupEntry

@Module
@InstallIn(SingletonComponent::class)
interface PlayerSetupMobileNavigationModule {

    @Binds
    fun bindPlayerSetupMobileNavRegistrar(impl: PlayerSetupMobileNavRegistrar): IMobilePlayerSetupEntry
}
