package su.afk.yummy.tv.feature.search.mobile.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import su.afk.yummy.tv.feature.search.IMobileSearchEntry
import su.afk.yummy.tv.feature.search.mobile.navigator.SearchNavRegistrar

@Module
@InstallIn(SingletonComponent::class)
interface SearchMobileNavigationModule {

    @Binds
    fun bindSearchNavRegistrar(impl: SearchNavRegistrar): IMobileSearchEntry
}
