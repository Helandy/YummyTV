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
import su.afk.yummy.tv.core.navigation.root.RootTab
import su.afk.yummy.tv.feature.account.IAccountNavigator
import su.afk.yummy.tv.feature.bloggers.IBloggerVideosNavigator
import su.afk.yummy.tv.feature.collection.ICollectionNavigator
import su.afk.yummy.tv.feature.comments.ICommentsNavigator
import su.afk.yummy.tv.feature.details.IDetailsNavigator
import su.afk.yummy.tv.feature.faq.IFaqNavigator
import su.afk.yummy.tv.feature.home.IHomeNavigator
import su.afk.yummy.tv.feature.library.ILibraryNavigator
import su.afk.yummy.tv.feature.messages.IMessagesNavigator
import su.afk.yummy.tv.feature.pages.ISitePagesNavigator
import su.afk.yummy.tv.feature.player.IPlayerNavigator
import su.afk.yummy.tv.feature.posts.IPostsNavigator
import su.afk.yummy.tv.feature.reviews.IReviewsNavigator
import su.afk.yummy.tv.feature.schedule.IScheduleNavigator
import su.afk.yummy.tv.feature.search.ISearchNavigator
import su.afk.yummy.tv.feature.settings.ISettingsNavigator
import su.afk.yummy.tv.feature.top.ITopNavigator
import su.afk.yummy.tv.feature.videodownload.IVideoDownloadNavigator
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object UniversalNavigationModule {

    @Provides
    fun provideRootTabs(): @JvmSuppressWildcards Map<RootTab, NavKey> = mapOf(
        RootTab.ACCOUNT to su.afk.yummy.tv.feature.account.navigator.AccountDestination,
        RootTab.SEARCH to su.afk.yummy.tv.feature.search.navigator.SearchDestination(),
        RootTab.HOME to su.afk.yummy.tv.feature.home.navigator.HomeDestination,
        RootTab.POSTS to su.afk.yummy.tv.feature.posts.navigator.PostsDestination,
        RootTab.COLLECTIONS to
                su.afk.yummy.tv.feature.collection.navigator.CollectionsCatalogDestination,
        RootTab.SCHEDULE to su.afk.yummy.tv.feature.schedule.navigator.ScheduleDestination,
        RootTab.TOP to su.afk.yummy.tv.feature.top.navigator.TopDestination,
        RootTab.LIBRARY to su.afk.yummy.tv.feature.library.navigator.LibraryDestination,
        RootTab.SETTINGS to su.afk.yummy.tv.feature.settings.navigator.SettingsDestination,
    )

    @Provides
    @Singleton
    fun provideAccountNavigator(): IAccountNavigator =
        su.afk.yummy.tv.feature.account.navigator.AccountNavigator()

    @Provides
    @Singleton
    fun provideMessagesNavigator(): IMessagesNavigator =
        su.afk.yummy.tv.feature.messages.navigator.MessagesNavigator()

    @Provides
    @Singleton
    fun provideCollectionNavigator(): ICollectionNavigator =
        su.afk.yummy.tv.feature.collection.navigator.CollectionNavigator()

    @Provides
    @Singleton
    fun provideCommentsNavigator(): ICommentsNavigator =
        su.afk.yummy.tv.feature.comments.navigator.CommentsNavigator()

    @Provides
    @Singleton
    fun provideDetailsNavigator(): IDetailsNavigator =
        su.afk.yummy.tv.feature.details.navigator.DetailsNavigator()

    @Provides
    @Singleton
    fun provideFaqNavigator(): IFaqNavigator =
        su.afk.yummy.tv.feature.faq.navigator.FaqNavigator()

    @Provides
    @Singleton
    fun provideSitePagesNavigator(): ISitePagesNavigator =
        su.afk.yummy.tv.feature.pages.navigator.SitePagesNavigator()

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
    fun providePostsNavigator(): IPostsNavigator =
        su.afk.yummy.tv.feature.posts.navigator.PostsNavigator()

    @Provides
    @Singleton
    fun provideReviewsNavigator(): IReviewsNavigator =
        su.afk.yummy.tv.feature.reviews.navigator.ReviewsNavigator()

    @Provides
    @Singleton
    fun provideBloggerVideosNavigator(): IBloggerVideosNavigator =
        su.afk.yummy.tv.feature.bloggers.navigator.BloggerVideosNavigator()

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
    fun provideTopNavigator(): ITopNavigator =
        su.afk.yummy.tv.feature.top.navigator.TopNavigator()

    @Provides
    @Singleton
    fun provideVideoDownloadNavigator(): IVideoDownloadNavigator =
        su.afk.yummy.tv.feature.videodownload.navigator.VideoDownloadNavigator()

    @Provides
    @IntoSet
    @MobileUi
    fun provideMobileAccountNavRegistrar(): NavRegistrar =
        su.afk.yummy.tv.feature.account.mobile.navigator.AccountNavRegistrar()

    @Provides
    @IntoSet
    @MobileUi
    fun provideMobileCollectionNavRegistrar(): NavRegistrar =
        su.afk.yummy.tv.feature.collection.mobile.navigator.CollectionNavRegistrar()

    @Provides
    @IntoSet
    @MobileUi
    fun provideMobileCommentsNavRegistrar(): NavRegistrar =
        su.afk.yummy.tv.feature.comments.mobile.navigator.CommentsNavRegistrar()

    @Provides
    @IntoSet
    @MobileUi
    fun provideMobileDetailsNavRegistrar(): NavRegistrar =
        su.afk.yummy.tv.feature.details.mobile.navigator.DetailsNavRegistrar()

    @Provides
    @IntoSet
    @MobileUi
    fun provideMobileFaqNavRegistrar(): NavRegistrar =
        su.afk.yummy.tv.feature.faq.mobile.navigator.FaqNavRegistrar()

    @Provides
    @IntoSet
    @MobileUi
    fun provideMobileSitePagesNavRegistrar(): NavRegistrar =
        su.afk.yummy.tv.feature.pages.mobile.navigator.SitePagesNavRegistrar()

    @Provides
    @IntoSet
    @MobileUi
    fun provideMobileHomeNavRegistrar(): NavRegistrar =
        su.afk.yummy.tv.feature.home.mobile.navigator.HomeNavRegistrar()

    @Provides
    @IntoSet
    @MobileUi
    fun provideMobileLibraryNavRegistrar(): NavRegistrar =
        su.afk.yummy.tv.feature.library.mobile.navigator.LibraryNavRegistrar()

    @Provides
    @IntoSet
    @MobileUi
    fun provideMobileMessagesNavRegistrar(): NavRegistrar =
        su.afk.yummy.tv.feature.messages.mobile.navigator.MessagesNavRegistrar()

    @Provides
    @IntoSet
    @MobileUi
    fun provideMobileReviewsNavRegistrar(): NavRegistrar =
        su.afk.yummy.tv.feature.reviews.mobile.navigator.ReviewsNavRegistrar()

    @Provides
    @IntoSet
    @MobileUi
    fun provideMobileBloggerVideosNavRegistrar(): NavRegistrar =
        su.afk.yummy.tv.feature.bloggers.mobile.navigator.BloggerVideosNavRegistrar()

    @Provides
    @IntoSet
    @MobileUi
    fun provideMobilePlayerNavRegistrar(): NavRegistrar =
        su.afk.yummy.tv.feature.player.mobile.navigator.PlayerNavRegistrar()

    @Provides
    @IntoSet
    @MobileUi
    fun provideMobilePostsNavRegistrar(): NavRegistrar =
        su.afk.yummy.tv.feature.posts.mobile.navigator.PostsNavRegistrar()

    @Provides
    @IntoSet
    @MobileUi
    fun provideMobileScheduleNavRegistrar(): NavRegistrar =
        su.afk.yummy.tv.feature.schedule.mobile.navigator.ScheduleNavRegistrar()

    @Provides
    @IntoSet
    @MobileUi
    fun provideMobileSearchNavRegistrar(): NavRegistrar =
        su.afk.yummy.tv.feature.search.mobile.navigator.SearchNavRegistrar()

    @Provides
    @IntoSet
    @MobileUi
    fun provideMobileSettingsNavRegistrar(): NavRegistrar =
        su.afk.yummy.tv.feature.settings.mobile.navigator.SettingsNavRegistrar()

    @Provides
    @IntoSet
    @MobileUi
    fun provideMobileTopNavRegistrar(): NavRegistrar =
        su.afk.yummy.tv.feature.top.mobile.navigator.TopNavRegistrar()

    @Provides
    @IntoSet
    @MobileUi
    fun provideMobileVideoDownloadNavRegistrar(): NavRegistrar =
        su.afk.yummy.tv.feature.videodownload.mobile.navigator.VideoDownloadNavRegistrar()

    @Provides
    @IntoSet
    @TvUi
    fun provideTvAccountNavRegistrar(): NavRegistrar =
        su.afk.yummy.tv.feature.account.tv.navigator.AccountNavRegistrar()

    @Provides
    @IntoSet
    @TvUi
    fun provideTvCollectionNavRegistrar(): NavRegistrar =
        su.afk.yummy.tv.feature.collection.tv.navigator.CollectionNavRegistrar()

    @Provides
    @IntoSet
    @TvUi
    fun provideTvCommentsNavRegistrar(): NavRegistrar =
        su.afk.yummy.tv.feature.comments.tv.navigator.CommentsNavRegistrar()

    @Provides
    @IntoSet
    @TvUi
    fun provideTvDetailsNavRegistrar(): NavRegistrar =
        su.afk.yummy.tv.feature.details.tv.navigator.DetailsNavRegistrar()

    @Provides
    @IntoSet
    @TvUi
    fun provideTvHomeNavRegistrar(): NavRegistrar =
        su.afk.yummy.tv.feature.home.tv.navigator.HomeNavRegistrar()

    @Provides
    @IntoSet
    @TvUi
    fun provideTvLibraryNavRegistrar(): NavRegistrar =
        su.afk.yummy.tv.feature.library.tv.navigator.LibraryNavRegistrar()

    @Provides
    @IntoSet
    @TvUi
    fun provideTvReviewsNavRegistrar(): NavRegistrar =
        su.afk.yummy.tv.feature.reviews.tv.navigator.ReviewsNavRegistrar()

    @Provides
    @IntoSet
    @TvUi
    fun provideTvBloggerVideosNavRegistrar(): NavRegistrar =
        su.afk.yummy.tv.feature.bloggers.tv.navigator.BloggerVideosNavRegistrar()

    @Provides
    @IntoSet
    @TvUi
    fun provideTvPlayerNavRegistrar(): NavRegistrar =
        su.afk.yummy.tv.feature.player.tv.navigator.PlayerNavRegistrar()

    @Provides
    @IntoSet
    @TvUi
    fun provideTvPostsNavRegistrar(): NavRegistrar =
        su.afk.yummy.tv.feature.posts.tv.navigator.PostsNavRegistrar()

    @Provides
    @IntoSet
    @TvUi
    fun provideTvScheduleNavRegistrar(): NavRegistrar =
        su.afk.yummy.tv.feature.schedule.tv.navigator.ScheduleNavRegistrar()

    @Provides
    @IntoSet
    @TvUi
    fun provideTvSearchNavRegistrar(): NavRegistrar =
        su.afk.yummy.tv.feature.search.tv.navigator.SearchNavRegistrar()

    @Provides
    @IntoSet
    @TvUi
    fun provideTvSettingsNavRegistrar(): NavRegistrar =
        su.afk.yummy.tv.feature.settings.tv.navigator.SettingsNavRegistrar()

    @Provides
    @IntoSet
    @TvUi
    fun provideTvTopNavRegistrar(): NavRegistrar =
        su.afk.yummy.tv.feature.top.tv.navigator.TopNavRegistrar()
}
