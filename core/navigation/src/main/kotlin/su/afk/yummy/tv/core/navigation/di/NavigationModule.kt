package su.afk.yummy.tv.core.navigation.di

import androidx.navigation3.runtime.NavKey
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import su.afk.yummy.tv.core.navigation.NavigationManager
import su.afk.yummy.tv.core.navigation.root.RootTab
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NavigationModule {

    @Provides
    fun provideInitialRoot(): RootTab = RootTab.HOME

    @Provides
    @Singleton
    fun provideNavigationManager(
        roots: @JvmSuppressWildcards Map<RootTab, NavKey>,
        initialRoot: RootTab,
    ): NavigationManager = NavigationManager(roots = roots, initialRoot = initialRoot)
}
