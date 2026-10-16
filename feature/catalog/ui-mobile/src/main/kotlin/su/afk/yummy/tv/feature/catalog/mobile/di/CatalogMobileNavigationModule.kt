package su.afk.yummy.tv.feature.catalog.mobile.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import su.afk.yummy.tv.feature.catalog.IMobileCatalogEntry
import su.afk.yummy.tv.feature.catalog.mobile.navigator.CatalogNavRegistrar

@Module
@InstallIn(SingletonComponent::class)
interface CatalogMobileNavigationModule {

    @Binds
    fun bindCatalogNavRegistrar(impl: CatalogNavRegistrar): IMobileCatalogEntry
}
