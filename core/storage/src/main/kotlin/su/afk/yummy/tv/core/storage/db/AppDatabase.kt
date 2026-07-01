package su.afk.yummy.tv.core.storage.db

import androidx.room.Database
import androidx.room.RoomDatabase
import su.afk.yummy.tv.core.storage.account.AccountAnimeListStateEntry
import su.afk.yummy.tv.core.storage.account.AccountCollectionItemEntry
import su.afk.yummy.tv.core.storage.account.AccountCollectionPageEntry
import su.afk.yummy.tv.core.storage.account.AccountListStatEntry
import su.afk.yummy.tv.core.storage.account.AccountListStatsCacheEntry
import su.afk.yummy.tv.core.storage.account.AccountNotificationAnimeEntry
import su.afk.yummy.tv.core.storage.account.AccountNotificationCountCacheEntry
import su.afk.yummy.tv.core.storage.account.AccountNotificationCountEntry
import su.afk.yummy.tv.core.storage.account.AccountNotificationEntry
import su.afk.yummy.tv.core.storage.account.AccountNotificationPageEntry
import su.afk.yummy.tv.core.storage.account.AccountProfileEntry
import su.afk.yummy.tv.core.storage.account.AccountRatingBucketCacheEntry
import su.afk.yummy.tv.core.storage.account.AccountRatingBucketEntry
import su.afk.yummy.tv.core.storage.account.AccountStorageDao
import su.afk.yummy.tv.core.storage.account.AccountUserFriendEntry
import su.afk.yummy.tv.core.storage.account.AccountUserGenreStatEntry
import su.afk.yummy.tv.core.storage.account.AccountUserListItemEntry
import su.afk.yummy.tv.core.storage.account.AccountUserListPageEntry
import su.afk.yummy.tv.core.storage.account.AccountUserListWatchStatEntry
import su.afk.yummy.tv.core.storage.account.AccountUserPostEntry
import su.afk.yummy.tv.core.storage.account.AccountUserProfileContentPageEntry
import su.afk.yummy.tv.core.storage.account.AccountUserProfileSummaryCacheEntry
import su.afk.yummy.tv.core.storage.account.AccountUserProfileWatchHistoryEntry
import su.afk.yummy.tv.core.storage.account.AccountUserProfileWatchTypeEntry
import su.afk.yummy.tv.core.storage.account.AccountUserRatingEntry
import su.afk.yummy.tv.core.storage.account.AccountUserRatingStatEntry
import su.afk.yummy.tv.core.storage.account.AccountUserReviewEntry
import su.afk.yummy.tv.core.storage.account.AccountUserStatsCacheEntry
import su.afk.yummy.tv.core.storage.account.AccountUserTypeStatEntry
import su.afk.yummy.tv.core.storage.account.AccountVideoSubscriptionCacheEntry
import su.afk.yummy.tv.core.storage.account.AccountVideoSubscriptionEntry
import su.afk.yummy.tv.core.storage.anime.AnimeDetailNamedEntry
import su.afk.yummy.tv.core.storage.anime.AnimeDetailTitleEntry
import su.afk.yummy.tv.core.storage.anime.AnimeDetailsEntry
import su.afk.yummy.tv.core.storage.anime.AnimeRecommendationCacheEntry
import su.afk.yummy.tv.core.storage.anime.AnimeRecommendationEntry
import su.afk.yummy.tv.core.storage.anime.AnimeScreenshotEntry
import su.afk.yummy.tv.core.storage.anime.AnimeStorageDao
import su.afk.yummy.tv.core.storage.anime.AnimeTrailerCacheEntry
import su.afk.yummy.tv.core.storage.anime.AnimeTrailerEntry
import su.afk.yummy.tv.core.storage.anime.AnimeVideoCacheEntry
import su.afk.yummy.tv.core.storage.anime.AnimeVideoEntry
import su.afk.yummy.tv.core.storage.anime.AnimeViewingOrderEntry
import su.afk.yummy.tv.core.storage.collection.CollectionAnimeItemEntry
import su.afk.yummy.tv.core.storage.collection.CollectionCatalogItemEntry
import su.afk.yummy.tv.core.storage.collection.CollectionCatalogPageEntry
import su.afk.yummy.tv.core.storage.collection.CollectionDetailEntry
import su.afk.yummy.tv.core.storage.collection.CollectionStorageDao
import su.afk.yummy.tv.core.storage.comments.CommentItemEntry
import su.afk.yummy.tv.core.storage.comments.CommentPageEntry
import su.afk.yummy.tv.core.storage.comments.CommentsStorageDao
import su.afk.yummy.tv.core.storage.home.HomeFeedCacheEntry
import su.afk.yummy.tv.core.storage.home.HomeFeedDao
import su.afk.yummy.tv.core.storage.home.HomeFeedItemEntry
import su.afk.yummy.tv.core.storage.library.LibraryDao
import su.afk.yummy.tv.core.storage.library.LibraryEntry
import su.afk.yummy.tv.core.storage.library.LibrarySyncStateEntry
import su.afk.yummy.tv.core.storage.schedule.AnimeScheduleCacheEntry
import su.afk.yummy.tv.core.storage.schedule.AnimeScheduleDao
import su.afk.yummy.tv.core.storage.schedule.AnimeScheduleItemEntry
import su.afk.yummy.tv.core.storage.search.SearchFilterOptionsEntry
import su.afk.yummy.tv.core.storage.search.SearchGenreEntry
import su.afk.yummy.tv.core.storage.search.SearchGenreGroupEntry
import su.afk.yummy.tv.core.storage.search.SearchItemEntry
import su.afk.yummy.tv.core.storage.search.SearchPageEntry
import su.afk.yummy.tv.core.storage.search.SearchStorageDao
import su.afk.yummy.tv.core.storage.search.SearchTypeEntry
import su.afk.yummy.tv.core.storage.top.AnimeTopDao
import su.afk.yummy.tv.core.storage.top.AnimeTopItemEntry
import su.afk.yummy.tv.core.storage.top.AnimeTopPageEntry
import su.afk.yummy.tv.core.storage.videodownload.VideoDownloadDao
import su.afk.yummy.tv.core.storage.videodownload.VideoDownloadEntry
import su.afk.yummy.tv.core.storage.watchprogress.ContinueWatchingSuppressionEntry
import su.afk.yummy.tv.core.storage.watchprogress.RemoteContinueWatchingDao
import su.afk.yummy.tv.core.storage.watchprogress.RemoteContinueWatchingEntry
import su.afk.yummy.tv.core.storage.watchprogress.WatchProgressDao
import su.afk.yummy.tv.core.storage.watchprogress.WatchProgressEntry

@Database(
    entities = [
        LibraryEntry::class,
        LibrarySyncStateEntry::class,
        WatchProgressEntry::class,
        ContinueWatchingSuppressionEntry::class,
        RemoteContinueWatchingEntry::class,
        AnimeDetailsEntry::class,
        AnimeDetailTitleEntry::class,
        AnimeDetailNamedEntry::class,
        AnimeViewingOrderEntry::class,
        AnimeScreenshotEntry::class,
        AnimeVideoCacheEntry::class,
        AnimeVideoEntry::class,
        AnimeRecommendationCacheEntry::class,
        AnimeRecommendationEntry::class,
        AnimeTrailerCacheEntry::class,
        AnimeTrailerEntry::class,
        HomeFeedCacheEntry::class,
        HomeFeedItemEntry::class,
        AnimeTopPageEntry::class,
        AnimeTopItemEntry::class,
        AnimeScheduleCacheEntry::class,
        AnimeScheduleItemEntry::class,
        SearchPageEntry::class,
        SearchItemEntry::class,
        SearchFilterOptionsEntry::class,
        SearchGenreGroupEntry::class,
        SearchGenreEntry::class,
        SearchTypeEntry::class,
        CollectionDetailEntry::class,
        CollectionAnimeItemEntry::class,
        CollectionCatalogPageEntry::class,
        CollectionCatalogItemEntry::class,
        AccountProfileEntry::class,
        AccountUserListPageEntry::class,
        AccountUserListItemEntry::class,
        AccountAnimeListStateEntry::class,
        AccountRatingBucketCacheEntry::class,
        AccountRatingBucketEntry::class,
        AccountUserRatingEntry::class,
        AccountListStatsCacheEntry::class,
        AccountListStatEntry::class,
        AccountCollectionPageEntry::class,
        AccountCollectionItemEntry::class,
        AccountVideoSubscriptionCacheEntry::class,
        AccountVideoSubscriptionEntry::class,
        AccountNotificationPageEntry::class,
        AccountNotificationEntry::class,
        AccountUserProfileContentPageEntry::class,
        AccountUserFriendEntry::class,
        AccountUserReviewEntry::class,
        AccountUserPostEntry::class,
        AccountNotificationCountCacheEntry::class,
        AccountNotificationCountEntry::class,
        AccountNotificationAnimeEntry::class,
        AccountUserStatsCacheEntry::class,
        AccountUserGenreStatEntry::class,
        AccountUserRatingStatEntry::class,
        AccountUserListWatchStatEntry::class,
        AccountUserTypeStatEntry::class,
        AccountUserProfileSummaryCacheEntry::class,
        AccountUserProfileWatchTypeEntry::class,
        AccountUserProfileWatchHistoryEntry::class,
        CommentPageEntry::class,
        CommentItemEntry::class,
        VideoDownloadEntry::class,
    ],
    version = 31,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun libraryDao(): LibraryDao
    abstract fun watchProgressDao(): WatchProgressDao
    abstract fun remoteContinueWatchingDao(): RemoteContinueWatchingDao
    abstract fun animeStorageDao(): AnimeStorageDao
    abstract fun homeFeedDao(): HomeFeedDao
    abstract fun animeTopDao(): AnimeTopDao
    abstract fun animeScheduleDao(): AnimeScheduleDao
    abstract fun searchStorageDao(): SearchStorageDao
    abstract fun collectionStorageDao(): CollectionStorageDao
    abstract fun accountStorageDao(): AccountStorageDao
    abstract fun commentsStorageDao(): CommentsStorageDao
    abstract fun videoDownloadDao(): VideoDownloadDao
}
