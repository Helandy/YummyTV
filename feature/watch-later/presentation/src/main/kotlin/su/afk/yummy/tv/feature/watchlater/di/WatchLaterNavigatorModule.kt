package su.afk.yummy.tv.feature.watchlater.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import su.afk.yummy.tv.feature.watchlater.IWatchLaterNavigator
import su.afk.yummy.tv.feature.watchlater.navigator.WatchLaterNavigator
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface WatchLaterNavigatorModule {

    @Binds
    @Singleton
    fun bindWatchLaterNavigator(impl: WatchLaterNavigator): IWatchLaterNavigator
}
