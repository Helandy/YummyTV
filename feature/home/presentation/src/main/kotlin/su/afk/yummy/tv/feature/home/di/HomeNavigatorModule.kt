package su.afk.yummy.tv.feature.home.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import su.afk.yummy.tv.feature.home.IHomeNavigator
import su.afk.yummy.tv.feature.home.navigator.HomeNavigator
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface HomeNavigatorModule {

    @Binds
    @Singleton
    fun bindHomeNavigator(impl: HomeNavigator): IHomeNavigator
}
