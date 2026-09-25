package su.afk.yummy.tv.feature.pages.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import su.afk.yummy.tv.feature.pages.ISitePagesNavigator
import su.afk.yummy.tv.feature.pages.navigator.SitePagesNavigator
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface PagesNavigatorModule {

    @Binds
    @Singleton
    fun bindSitePagesNavigator(impl: SitePagesNavigator): ISitePagesNavigator
}
