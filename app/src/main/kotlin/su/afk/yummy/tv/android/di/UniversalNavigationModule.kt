package su.afk.yummy.tv.android.di

import androidx.navigation3.runtime.NavKey
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import su.afk.yummy.tv.core.navigation.MobileUi
import su.afk.yummy.tv.core.navigation.NavRegistrar
import su.afk.yummy.tv.core.navigation.TvUi
import su.afk.yummy.tv.core.navigation.tab.SideTab
import su.afk.yummy.tv.feature.account.IAccountNavigator
import su.afk.yummy.tv.feature.collection.ICollectionNavigator
import su.afk.yummy.tv.feature.details.IDetailsNavigator
import su.afk.yummy.tv.feature.home.IHomeNavigator
import su.afk.yummy.tv.feature.library.ILibraryNavigator
import su.afk.yummy.tv.feature.player.IPlayerNavigator
import su.afk.yummy.tv.feature.schedule.IScheduleNavigator
import su.afk.yummy.tv.feature.search.ISearchNavigator
import su.afk.yummy.tv.feature.settings.ISettingsNavigator
import su.afk.yummy.tv.feature.top100.ITop100Navigator
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object UniversalNavigationModule {

    @Provides
    fun provideTabRoots(): @JvmSuppressWildcards Map<SideTab, NavKey> = mapOf(
        SideTab.HOME to su.afk.yummy.tv.feature.home.navigator.HomeDestination,
        SideTab.SEARCH to su.afk.yummy.tv.feature.search.navigator.SearchDestination,
        SideTab.SCHEDULE to su.afk.yummy.tv.feature.schedule.navigator.ScheduleDestination,
        SideTab.TOP100 to su.afk.yummy.tv.feature.top100.navigator.Top100Destination,
        SideTab.LIBRARY to su.afk.yummy.tv.feature.library.navigator.LibraryDestination,
    )

    @Provides
    @Singleton
    fun provideAccountNavigator(): IAccountNavigator =
        su.afk.yummy.tv.feature.account.navigator.AccountNavigator()

    @Provides
    @Singleton
    fun provideCollectionNavigator(): ICollectionNavigator =
        su.afk.yummy.tv.feature.collection.navigator.CollectionNavigator()

    @Provides
    @Singleton
    fun provideDetailsNavigator(): IDetailsNavigator =
        su.afk.yummy.tv.feature.details.navigator.DetailsNavigator()

    @Provides
    @Singleton
    fun provideHomeNavigator(): IHomeNavigator =
        su.afk.yummy.tv.feature.home.navigator.HomeNavigator()

    @Provides
    @Singleton
    fun provideLibraryNavigator(): ILibraryNavigator =
        su.afk.yummy.tv.feature.library.navigator.LibraryNavigator()

    @Provides
    @Singleton
    fun providePlayerNavigator(): IPlayerNavigator =
        su.afk.yummy.tv.feature.player.navigator.PlayerNavigator()

    @Provides
    @Singleton
    fun provideScheduleNavigator(): IScheduleNavigator =
        su.afk.yummy.tv.feature.schedule.navigator.ScheduleNavigator()

    @Provides
    @Singleton
    fun provideSearchNavigator(): ISearchNavigator =
        su.afk.yummy.tv.feature.search.navigator.SearchNavigator()

    @Provides
    @Singleton
    fun provideSettingsNavigator(): ISettingsNavigator =
        su.afk.yummy.tv.feature.settings.navigator.SettingsNavigator()

    @Provides
    @Singleton
    fun provideTop100Navigator(): ITop100Navigator =
        su.afk.yummy.tv.feature.top100.navigator.Top100Navigator()

    @Provides @IntoSet @MobileUi
    fun provideMobileAccountNavRegistrar(): NavRegistrar =
        su.afk.yummy.tv.feature.account.mobile.navigator.AccountNavRegistrar()

    @Provides @IntoSet @MobileUi
    fun provideMobileCollectionNavRegistrar(): NavRegistrar =
        su.afk.yummy.tv.feature.collection.mobile.navigator.CollectionNavRegistrar()

    @Provides @IntoSet @MobileUi
    fun provideMobileDetailsNavRegistrar(): NavRegistrar =
        su.afk.yummy.tv.feature.details.mobile.navigator.DetailsNavRegistrar()

    @Provides @IntoSet @MobileUi
    fun provideMobileHomeNavRegistrar(): NavRegistrar =
        su.afk.yummy.tv.feature.home.mobile.navigator.HomeNavRegistrar()

    @Provides @IntoSet @MobileUi
    fun provideMobileLibraryNavRegistrar(): NavRegistrar =
        su.afk.yummy.tv.feature.library.mobile.navigator.LibraryNavRegistrar()

    @Provides @IntoSet @MobileUi
    fun provideMobilePlayerNavRegistrar(): NavRegistrar =
        su.afk.yummy.tv.feature.player.mobile.navigator.PlayerNavRegistrar()

    @Provides @IntoSet @MobileUi
    fun provideMobileScheduleNavRegistrar(): NavRegistrar =
        su.afk.yummy.tv.feature.schedule.mobile.navigator.ScheduleNavRegistrar()

    @Provides @IntoSet @MobileUi
    fun provideMobileSearchNavRegistrar(): NavRegistrar =
        su.afk.yummy.tv.feature.search.mobile.navigator.SearchNavRegistrar()

    @Provides @IntoSet @MobileUi
    fun provideMobileSettingsNavRegistrar(): NavRegistrar =
        su.afk.yummy.tv.feature.settings.mobile.navigator.SettingsNavRegistrar()

    @Provides @IntoSet @MobileUi
    fun provideMobileTop100NavRegistrar(): NavRegistrar =
        su.afk.yummy.tv.feature.top100.mobile.navigator.Top100NavRegistrar()

    @Provides @IntoSet @TvUi
    fun provideTvAccountNavRegistrar(): NavRegistrar =
        su.afk.yummy.tv.feature.account.tv.navigator.AccountNavRegistrar()

    @Provides @IntoSet @TvUi
    fun provideTvCollectionNavRegistrar(): NavRegistrar =
        su.afk.yummy.tv.feature.collection.tv.navigator.CollectionNavRegistrar()

    @Provides @IntoSet @TvUi
    fun provideTvDetailsNavRegistrar(): NavRegistrar =
        su.afk.yummy.tv.feature.details.tv.navigator.DetailsNavRegistrar()

    @Provides @IntoSet @TvUi
    fun provideTvHomeNavRegistrar(): NavRegistrar =
        su.afk.yummy.tv.feature.home.tv.navigator.HomeNavRegistrar()

    @Provides @IntoSet @TvUi
    fun provideTvLibraryNavRegistrar(): NavRegistrar =
        su.afk.yummy.tv.feature.library.tv.navigator.LibraryNavRegistrar()

    @Provides @IntoSet @TvUi
    fun provideTvPlayerNavRegistrar(): NavRegistrar =
        su.afk.yummy.tv.feature.player.tv.navigator.PlayerNavRegistrar()

    @Provides @IntoSet @TvUi
    fun provideTvScheduleNavRegistrar(): NavRegistrar =
        su.afk.yummy.tv.feature.schedule.tv.navigator.ScheduleNavRegistrar()

    @Provides @IntoSet @TvUi
    fun provideTvSearchNavRegistrar(): NavRegistrar =
        su.afk.yummy.tv.feature.search.tv.navigator.SearchNavRegistrar()

    @Provides @IntoSet @TvUi
    fun provideTvSettingsNavRegistrar(): NavRegistrar =
        su.afk.yummy.tv.feature.settings.tv.navigator.SettingsNavRegistrar()

    @Provides @IntoSet @TvUi
    fun provideTvTop100NavRegistrar(): NavRegistrar =
        su.afk.yummy.tv.feature.top100.tv.navigator.Top100NavRegistrar()
}
