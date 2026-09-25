package su.afk.yummy.tv.feature.main.di

import androidx.navigation3.runtime.NavKey
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import su.afk.yummy.tv.core.navigation.root.RootTab
import su.afk.yummy.tv.feature.account.navigator.AccountDestination
import su.afk.yummy.tv.feature.collection.navigator.CollectionsCatalogDestination
import su.afk.yummy.tv.feature.home.navigator.HomeDestination
import su.afk.yummy.tv.feature.library.navigator.LibraryDestination
import su.afk.yummy.tv.feature.posts.navigator.PostsDestination
import su.afk.yummy.tv.feature.schedule.navigator.ScheduleDestination
import su.afk.yummy.tv.feature.search.navigator.SearchDestination
import su.afk.yummy.tv.feature.settings.navigator.SettingsDestination
import su.afk.yummy.tv.feature.top.navigator.TopDestination

/** Корневой экран каждого таба; общий для mobile и TV, набор табов определяет main-фича. */
@Module
@InstallIn(SingletonComponent::class)
internal object RootTabsModule {

    @Provides
    fun provideRootTabs(): @JvmSuppressWildcards Map<RootTab, NavKey> = mapOf(
        RootTab.ACCOUNT to AccountDestination,
        RootTab.SEARCH to SearchDestination(),
        RootTab.HOME to HomeDestination,
        RootTab.POSTS to PostsDestination,
        RootTab.COLLECTIONS to CollectionsCatalogDestination,
        RootTab.SCHEDULE to ScheduleDestination,
        RootTab.TOP to TopDestination,
        RootTab.LIBRARY to LibraryDestination,
        RootTab.SETTINGS to SettingsDestination,
    )
}
