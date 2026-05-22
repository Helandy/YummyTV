package su.afk.yummy.tv.feature.main.di

import androidx.navigation3.runtime.NavKey
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import su.afk.yummy.tv.core.navigation.tab.SideTab
import su.afk.yummy.tv.feature.home.navigator.HomeDestination
import su.afk.yummy.tv.feature.library.navigator.LibraryDestination
import su.afk.yummy.tv.feature.search.navigator.SearchDestination
import su.afk.yummy.tv.feature.top100.navigator.Top100Destination

@Module
@InstallIn(SingletonComponent::class)
object TvNavigationModule {

    @Provides
    fun provideTabRoots(): @JvmSuppressWildcards Map<SideTab, NavKey> = mapOf(
        SideTab.HOME to HomeDestination,
        SideTab.SEARCH to SearchDestination,
        SideTab.TOP100 to Top100Destination,
        SideTab.LIBRARY to LibraryDestination,
    )
}
