package su.afk.yummy.tv.feature.pages.mobile.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import su.afk.yummy.tv.feature.pages.IMobileSitePagesEntry
import su.afk.yummy.tv.feature.pages.mobile.navigator.SitePagesNavRegistrar

@Module
@InstallIn(SingletonComponent::class)
interface SitePagesMobileNavigationModule {

    @Binds
    fun bindSitePagesNavRegistrar(impl: SitePagesNavRegistrar): IMobileSitePagesEntry
}
