package su.afk.yummy.tv.feature.top.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import su.afk.yummy.tv.feature.top.ITopNavigator
import su.afk.yummy.tv.feature.top.navigator.TopNavigator
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface TopNavigatorModule {

    @Binds
    @Singleton
    fun bindTopNavigator(impl: TopNavigator): ITopNavigator
}
