package su.afk.yummy.tv.feature.main.mobile.navigation

import su.afk.yummy.tv.core.navigation.registrar.NavRegistrar
import su.afk.yummy.tv.feature.account.IMobileAccountEntry
import su.afk.yummy.tv.feature.bloggers.IMobileBloggerVideosEntry
import su.afk.yummy.tv.feature.collection.IMobileCollectionEntry
import su.afk.yummy.tv.feature.comments.IMobileCommentsEntry
import su.afk.yummy.tv.feature.commonscreen.navigator.IErrorScreenEntry
import su.afk.yummy.tv.feature.commonscreen.navigator.IImageViewEntry
import su.afk.yummy.tv.feature.details.IMobileDetailsEntry
import su.afk.yummy.tv.feature.faq.IMobileFaqEntry
import su.afk.yummy.tv.feature.home.IMobileHomeEntry
import su.afk.yummy.tv.feature.library.IMobileLibraryEntry
import su.afk.yummy.tv.feature.messages.IMobileMessagesEntry
import su.afk.yummy.tv.feature.pages.IMobileSitePagesEntry
import su.afk.yummy.tv.feature.player.IMobilePlayerEntry
import su.afk.yummy.tv.feature.posts.IMobilePostsEntry
import su.afk.yummy.tv.feature.reviews.IMobileReviewsEntry
import su.afk.yummy.tv.feature.schedule.IMobileScheduleEntry
import su.afk.yummy.tv.feature.search.IMobileSearchEntry
import su.afk.yummy.tv.feature.settings.IMobileSettingsEntry
import su.afk.yummy.tv.feature.top.IMobileTopEntry
import su.afk.yummy.tv.feature.update.navigator.IUpdateEntry
import su.afk.yummy.tv.feature.videodownload.IMobileVideoDownloadEntry
import su.afk.yummy.tv.feature.watchlater.IMobileWatchLaterEntry
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Явный список экранов мобильного графа. Каждая фича отдаёт свою точку входа через
 * `api`-контракт, поэтому забытый биндинг — ошибка Dagger на компиляции, а не отсутствующий
 * экран в рантайме.
 */
@Singleton
class MobileNavigationHolder @Inject constructor(
    account: IMobileAccountEntry,
    bloggerVideos: IMobileBloggerVideosEntry,
    collection: IMobileCollectionEntry,
    comments: IMobileCommentsEntry,
    details: IMobileDetailsEntry,
    faq: IMobileFaqEntry,
    home: IMobileHomeEntry,
    library: IMobileLibraryEntry,
    messages: IMobileMessagesEntry,
    sitePages: IMobileSitePagesEntry,
    player: IMobilePlayerEntry,
    posts: IMobilePostsEntry,
    reviews: IMobileReviewsEntry,
    schedule: IMobileScheduleEntry,
    search: IMobileSearchEntry,
    settings: IMobileSettingsEntry,
    top: IMobileTopEntry,
    videoDownload: IMobileVideoDownloadEntry,
    watchLater: IMobileWatchLaterEntry,
    update: IUpdateEntry,
    errorScreen: IErrorScreenEntry,
    imageView: IImageViewEntry,
) {
    val registrars: Set<NavRegistrar> = setOf(
        account,
        bloggerVideos,
        collection,
        comments,
        details,
        faq,
        home,
        library,
        messages,
        sitePages,
        player,
        posts,
        reviews,
        schedule,
        search,
        settings,
        top,
        videoDownload,
        watchLater,
        update,
        errorScreen,
        imageView,
    )
}
