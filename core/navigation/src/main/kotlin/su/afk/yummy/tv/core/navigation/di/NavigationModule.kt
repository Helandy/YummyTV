package su.afk.yummy.tv.core.navigation.di

import androidx.navigation3.runtime.NavKey
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import su.afk.yummy.tv.core.navigation.NavigationManager
import su.afk.yummy.tv.core.navigation.tab.SideTab
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NavigationModule {

    @Provides
    fun provideInitialTab(): SideTab = SideTab.HOME

    @Provides
    @Singleton
    fun provideNavigationManager(
        roots: @JvmSuppressWildcards Map<SideTab, NavKey>,
        initialTab: SideTab,
    ): NavigationManager = NavigationManager(roots = roots, initialTab = initialTab)
}
