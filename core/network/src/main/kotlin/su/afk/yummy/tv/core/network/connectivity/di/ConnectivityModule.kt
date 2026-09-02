package su.afk.yummy.tv.core.network.connectivity.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import su.afk.yummy.tv.core.network.connectivity.AndroidNetworkConnectivityMonitor
import su.afk.yummy.tv.core.network.connectivity.NetworkConnectivityMonitor
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface ConnectivityModule {

    @Binds
    @Singleton
    fun bindNetworkConnectivityMonitor(
        implementation: AndroidNetworkConnectivityMonitor,
    ): NetworkConnectivityMonitor
}
