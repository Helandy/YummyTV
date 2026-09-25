package su.afk.yummy.tv.feature.watchlater.mobile.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import su.afk.yummy.tv.feature.watchlater.IMobileWatchLaterEntry
import su.afk.yummy.tv.feature.watchlater.mobile.navigator.WatchLaterNavRegistrar

@Module
@InstallIn(SingletonComponent::class)
interface WatchLaterMobileNavigationModule {

    @Binds
    fun bindWatchLaterNavRegistrar(impl: WatchLaterNavRegistrar): IMobileWatchLaterEntry
}
