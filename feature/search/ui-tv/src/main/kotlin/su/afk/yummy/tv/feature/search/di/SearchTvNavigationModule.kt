package su.afk.yummy.tv.feature.search.tv.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import su.afk.yummy.tv.feature.search.ITvSearchEntry
import su.afk.yummy.tv.feature.search.tv.navigator.SearchNavRegistrar

@Module
@InstallIn(SingletonComponent::class)
interface SearchTvNavigationModule {

    @Binds
    fun bindSearchNavRegistrar(impl: SearchNavRegistrar): ITvSearchEntry
}
