package su.afk.yummy.tv.core.navigation.di

import androidx.navigation3.runtime.NavKey
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import su.afk.yummy.tv.core.navigation.manager.BackStackHost
import su.afk.yummy.tv.core.navigation.manager.INavigationManager
import su.afk.yummy.tv.core.navigation.manager.NavigationManager
import su.afk.yummy.tv.core.navigation.root.RootTab
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NavManagerDiModule {

    @Provides
    fun provideInitialRoot(): RootTab = RootTab.HOME

    @Provides
    @Singleton
    internal fun provideNavigationManager(
        roots: @JvmSuppressWildcards Map<RootTab, NavKey>,
        initialRoot: RootTab,
    ): NavigationManager = NavigationManager(roots = roots, initialRoot = initialRoot)

    @Provides
    @Singleton
    internal fun provideINavigationManager(navigationManager: NavigationManager): INavigationManager =
        navigationManager

    @Provides
    @Singleton
    internal fun provideBackStackHost(navigationManager: NavigationManager): BackStackHost =
        navigationManager
}
