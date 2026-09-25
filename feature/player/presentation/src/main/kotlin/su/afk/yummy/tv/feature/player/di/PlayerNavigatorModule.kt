package su.afk.yummy.tv.feature.player.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import su.afk.yummy.tv.feature.player.IPlayerNavigator
import su.afk.yummy.tv.feature.player.navigator.PlayerNavigator
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface PlayerNavigatorModule {

    @Binds
    @Singleton
    fun bindPlayerNavigator(impl: PlayerNavigator): IPlayerNavigator
}
