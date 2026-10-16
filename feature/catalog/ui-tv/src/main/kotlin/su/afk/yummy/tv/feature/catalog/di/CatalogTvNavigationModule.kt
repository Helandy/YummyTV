package su.afk.yummy.tv.feature.catalog.tv.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import su.afk.yummy.tv.feature.catalog.ITvCatalogEntry
import su.afk.yummy.tv.feature.catalog.tv.navigator.CatalogNavRegistrar

@Module
@InstallIn(SingletonComponent::class)
interface CatalogTvNavigationModule {

    @Binds
    fun bindCatalogNavRegistrar(impl: CatalogNavRegistrar): ITvCatalogEntry
}
