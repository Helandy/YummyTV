package su.afk.yummy.tv.feature.main.navigation

import su.afk.yummy.tv.core.navigation.registrar.NavRegistrar
import su.afk.yummy.tv.feature.account.ITvAccountEntry
import su.afk.yummy.tv.feature.bloggers.ITvBloggerVideosEntry
import su.afk.yummy.tv.feature.collection.ITvCollectionEntry
import su.afk.yummy.tv.feature.comments.ITvCommentsEntry
import su.afk.yummy.tv.feature.commonscreen.navigator.IErrorScreenEntry
import su.afk.yummy.tv.feature.commonscreen.navigator.IImageViewEntry
import su.afk.yummy.tv.feature.details.ITvDetailsEntry
import su.afk.yummy.tv.feature.home.ITvHomeEntry
import su.afk.yummy.tv.feature.library.ITvLibraryEntry
import su.afk.yummy.tv.feature.player.ITvPlayerEntry
import su.afk.yummy.tv.feature.posts.ITvPostsEntry
import su.afk.yummy.tv.feature.reviews.ITvReviewsEntry
import su.afk.yummy.tv.feature.schedule.ITvScheduleEntry
import su.afk.yummy.tv.feature.search.ITvSearchEntry
import su.afk.yummy.tv.feature.settings.ITvSettingsEntry
import su.afk.yummy.tv.feature.top.ITvTopEntry
import su.afk.yummy.tv.feature.update.navigator.IUpdateEntry
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Явный список экранов TV-графа. Каждая фича отдаёт свою точку входа через `api`-контракт,
 * поэтому забытый биндинг — ошибка Dagger на компиляции, а не отсутствующий экран в рантайме.
 */
@Singleton
class TvNavigationHolder @Inject constructor(
    account: ITvAccountEntry,
    bloggerVideos: ITvBloggerVideosEntry,
    collection: ITvCollectionEntry,
    comments: ITvCommentsEntry,
    details: ITvDetailsEntry,
    home: ITvHomeEntry,
    library: ITvLibraryEntry,
    player: ITvPlayerEntry,
    posts: ITvPostsEntry,
    reviews: ITvReviewsEntry,
    schedule: ITvScheduleEntry,
    search: ITvSearchEntry,
    settings: ITvSettingsEntry,
    top: ITvTopEntry,
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
        home,
        library,
        player,
        posts,
        reviews,
        schedule,
        search,
        settings,
        top,
        update,
        errorScreen,
        imageView,
    )
}
