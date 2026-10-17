package su.afk.yummy.tv.feature.catalog.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import su.afk.yummy.tv.feature.catalog.ICatalogNavigator
import su.afk.yummy.tv.feature.catalog.ICatalogSearchOpener
import su.afk.yummy.tv.feature.catalog.navigator.CatalogNavigator
import su.afk.yummy.tv.feature.catalog.navigator.CatalogSearchOpener
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface CatalogNavigatorModule {

    @Binds
    @Singleton
    fun bindCatalogNavigator(impl: CatalogNavigator): ICatalogNavigator

    @Binds
    @Singleton
    fun bindCatalogSearchOpener(impl: CatalogSearchOpener): ICatalogSearchOpener
}
