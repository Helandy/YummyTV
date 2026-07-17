package su.afk.yummy.tv.core.storage.maintenance

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction

/**
 * Purges stale cache rows across the whole database.
 *
 * Every cache group is cleaned in two steps: parent rows older than the retention
 * threshold are deleted by `cachedAt`, then child rows whose parent no longer exists
 * are removed as orphans. User-owned tables (library, watch_progress, video_downloads,
 * continue watching, sync states) are intentionally left untouched.
 */
@Dao
abstract class StorageCleanupDao {

    @Transaction
    open suspend fun purgeCachesOlderThan(minCachedAt: Long) {
        deleteStaleAnimeDetails(minCachedAt)
        deleteOrphanAnimeDetailTitles()
        deleteOrphanAnimeDetailNamedItems()
        deleteOrphanAnimeViewingOrder()
        deleteOrphanAnimeScreenshots()

        deleteStaleAnimeVideoCaches(minCachedAt)
        deleteOrphanAnimeVideos()

        deleteStaleAnimeRecommendationCaches(minCachedAt)
        deleteOrphanAnimeRecommendations()

        deleteStaleAnimeTrailerCaches(minCachedAt)
        deleteOrphanAnimeTrailers()

        deleteStaleHomeFeedCaches(minCachedAt)
        deleteOrphanHomeFeedItems()

        deleteStaleAnimeTopPages(minCachedAt)
        deleteOrphanAnimeTopItems()

        deleteStaleAnimeScheduleCaches(minCachedAt)
        deleteOrphanAnimeScheduleItems()

        deleteStaleSearchPages(minCachedAt)
        deleteOrphanSearchItems()

        deleteStaleSearchFilterOptions(minCachedAt)
        deleteOrphanSearchGenreGroups()
        deleteOrphanSearchGenres()
        deleteOrphanSearchTypes()

        deleteStaleCollectionDetails(minCachedAt)
        deleteOrphanCollectionAnimeItems()

        deleteStaleCollectionCatalogPages(minCachedAt)
        deleteOrphanCollectionCatalogItems()

        deleteStaleCommentPages(minCachedAt)
        deleteOrphanCommentItems()

        deleteStaleAccountProfiles(minCachedAt)
        deleteStaleAccountAnimeListStates(minCachedAt)
        deleteStaleAccountUserRatings(minCachedAt)
        deleteStaleAccountNotificationAnime(minCachedAt)

        deleteStaleAccountUserListPages(minCachedAt)
        deleteOrphanAccountUserListItems()

        deleteStaleAccountRatingBucketCaches(minCachedAt)
        deleteOrphanAccountRatingBuckets()

        deleteStaleAccountListStatsCaches(minCachedAt)
        deleteOrphanAccountListStats()

        deleteStaleAccountCollectionPages(minCachedAt)
        deleteOrphanAccountCollectionItems()

        deleteStaleAccountVideoSubscriptionCaches(minCachedAt)
        deleteOrphanAccountVideoSubscriptions()

        deleteStaleAccountNotificationPages(minCachedAt)
        deleteOrphanAccountNotifications()

        deleteStaleAccountNotificationCountCaches(minCachedAt)
        deleteOrphanAccountNotificationCounts()

        deleteStaleAccountUserProfileContentPages(minCachedAt)
        deleteOrphanAccountUserFriends()
        deleteOrphanAccountUserReviews()
        deleteOrphanAccountUserPosts()

        deleteStaleAccountUserStatsCaches(minCachedAt)
        deleteOrphanAccountUserGenreStats()
        deleteOrphanAccountUserRatingStats()
        deleteOrphanAccountUserListWatchStats()
        deleteOrphanAccountUserTypeStats()

        deleteStaleAccountUserProfileSummaryCaches(minCachedAt)
        deleteOrphanAccountUserProfileWatchTypes()
        deleteOrphanAccountUserProfileWatchHistory()

        deleteStaleDocuments(minCachedAt)
    }

    @Query("DELETE FROM document_cache WHERE cachedAt < :minCachedAt")
    abstract suspend fun deleteStaleDocuments(minCachedAt: Long)

    // region anime details

    @Query("DELETE FROM anime_details WHERE cachedAt < :minCachedAt")
    abstract suspend fun deleteStaleAnimeDetails(minCachedAt: Long)

    @Query(
        """
        DELETE FROM anime_detail_titles
        WHERE NOT EXISTS (
            SELECT 1 FROM anime_details AS parent
            WHERE parent.animeId = anime_detail_titles.animeId
                AND parent.language = anime_detail_titles.language
        )
        """
    )
    abstract suspend fun deleteOrphanAnimeDetailTitles()

    @Query(
        """
        DELETE FROM anime_detail_named_items
        WHERE NOT EXISTS (
            SELECT 1 FROM anime_details AS parent
            WHERE parent.animeId = anime_detail_named_items.animeId
                AND parent.language = anime_detail_named_items.language
        )
        """
    )
    abstract suspend fun deleteOrphanAnimeDetailNamedItems()

    @Query(
        """
        DELETE FROM anime_viewing_order
        WHERE NOT EXISTS (
            SELECT 1 FROM anime_details AS parent
            WHERE parent.animeId = anime_viewing_order.animeId
                AND parent.language = anime_viewing_order.language
        )
        """
    )
    abstract suspend fun deleteOrphanAnimeViewingOrder()

    @Query(
        """
        DELETE FROM anime_screenshots
        WHERE NOT EXISTS (
            SELECT 1 FROM anime_details AS parent
            WHERE parent.animeId = anime_screenshots.animeId
                AND parent.language = anime_screenshots.language
        )
        """
    )
    abstract suspend fun deleteOrphanAnimeScreenshots()

    // endregion

    // region anime videos / recommendations / trailers

    @Query("DELETE FROM anime_video_caches WHERE cachedAt < :minCachedAt")
    abstract suspend fun deleteStaleAnimeVideoCaches(minCachedAt: Long)

    @Query(
        """
        DELETE FROM anime_videos
        WHERE NOT EXISTS (
            SELECT 1 FROM anime_video_caches AS parent
            WHERE parent.animeId = anime_videos.animeId
                AND parent.language = anime_videos.language
        )
        """
    )
    abstract suspend fun deleteOrphanAnimeVideos()

    @Query("DELETE FROM anime_recommendation_caches WHERE cachedAt < :minCachedAt")
    abstract suspend fun deleteStaleAnimeRecommendationCaches(minCachedAt: Long)

    @Query(
        """
        DELETE FROM anime_recommendations
        WHERE NOT EXISTS (
            SELECT 1 FROM anime_recommendation_caches AS parent
            WHERE parent.animeId = anime_recommendations.animeId
                AND parent.language = anime_recommendations.language
                AND parent.fromAi = anime_recommendations.fromAi
        )
        """
    )
    abstract suspend fun deleteOrphanAnimeRecommendations()

    @Query("DELETE FROM anime_trailer_caches WHERE cachedAt < :minCachedAt")
    abstract suspend fun deleteStaleAnimeTrailerCaches(minCachedAt: Long)

    @Query(
        """
        DELETE FROM anime_trailers
        WHERE NOT EXISTS (
            SELECT 1 FROM anime_trailer_caches AS parent
            WHERE parent.animeId = anime_trailers.animeId
                AND parent.language = anime_trailers.language
        )
        """
    )
    abstract suspend fun deleteOrphanAnimeTrailers()

    // endregion

    // region home / top / schedule

    @Query("DELETE FROM home_feed_caches WHERE cachedAt < :minCachedAt")
    abstract suspend fun deleteStaleHomeFeedCaches(minCachedAt: Long)

    @Query(
        """
        DELETE FROM home_feed_items
        WHERE NOT EXISTS (
            SELECT 1 FROM home_feed_caches AS parent
            WHERE parent.language = home_feed_items.language
                AND parent.watchSignature = home_feed_items.watchSignature
        )
        """
    )
    abstract suspend fun deleteOrphanHomeFeedItems()

    @Query("DELETE FROM anime_top_pages WHERE cachedAt < :minCachedAt")
    abstract suspend fun deleteStaleAnimeTopPages(minCachedAt: Long)

    @Query(
        """
        DELETE FROM anime_top_items
        WHERE NOT EXISTS (
            SELECT 1 FROM anime_top_pages AS parent
            WHERE parent.type = anime_top_items.type
                AND parent.language = anime_top_items.language
                AND parent.`limit` = anime_top_items.`limit`
                AND parent.`offset` = anime_top_items.`offset`
        )
        """
    )
    abstract suspend fun deleteOrphanAnimeTopItems()

    @Query("DELETE FROM anime_schedule_caches WHERE cachedAt < :minCachedAt")
    abstract suspend fun deleteStaleAnimeScheduleCaches(minCachedAt: Long)

    @Query(
        """
        DELETE FROM anime_schedule_items
        WHERE NOT EXISTS (
            SELECT 1 FROM anime_schedule_caches AS parent
            WHERE parent.language = anime_schedule_items.language
        )
        """
    )
    abstract suspend fun deleteOrphanAnimeScheduleItems()

    // endregion

    // region search

    @Query("DELETE FROM search_pages WHERE cachedAt < :minCachedAt")
    abstract suspend fun deleteStaleSearchPages(minCachedAt: Long)

    @Query(
        """
        DELETE FROM search_items
        WHERE NOT EXISTS (
            SELECT 1 FROM search_pages AS parent
            WHERE parent.pageKey = search_items.pageKey
        )
        """
    )
    abstract suspend fun deleteOrphanSearchItems()

    @Query("DELETE FROM search_filter_options WHERE cachedAt < :minCachedAt")
    abstract suspend fun deleteStaleSearchFilterOptions(minCachedAt: Long)

    @Query(
        """
        DELETE FROM search_genre_groups
        WHERE NOT EXISTS (
            SELECT 1 FROM search_filter_options AS parent
            WHERE parent.language = search_genre_groups.language
        )
        """
    )
    abstract suspend fun deleteOrphanSearchGenreGroups()

    @Query(
        """
        DELETE FROM search_genres
        WHERE NOT EXISTS (
            SELECT 1 FROM search_filter_options AS parent
            WHERE parent.language = search_genres.language
        )
        """
    )
    abstract suspend fun deleteOrphanSearchGenres()

    @Query(
        """
        DELETE FROM search_types
        WHERE NOT EXISTS (
            SELECT 1 FROM search_filter_options AS parent
            WHERE parent.language = search_types.language
        )
        """
    )
    abstract suspend fun deleteOrphanSearchTypes()

    // endregion

    // region collections

    @Query("DELETE FROM collection_details WHERE cachedAt < :minCachedAt")
    abstract suspend fun deleteStaleCollectionDetails(minCachedAt: Long)

    @Query(
        """
        DELETE FROM collection_anime_items
        WHERE NOT EXISTS (
            SELECT 1 FROM collection_details AS parent
            WHERE parent.collectionId = collection_anime_items.collectionId
                AND parent.language = collection_anime_items.language
        )
        """
    )
    abstract suspend fun deleteOrphanCollectionAnimeItems()

    @Query("DELETE FROM collection_catalog_pages WHERE cachedAt < :minCachedAt")
    abstract suspend fun deleteStaleCollectionCatalogPages(minCachedAt: Long)

    @Query(
        """
        DELETE FROM collection_catalog_items
        WHERE NOT EXISTS (
            SELECT 1 FROM collection_catalog_pages AS parent
            WHERE parent.pageKey = collection_catalog_items.pageKey
        )
        """
    )
    abstract suspend fun deleteOrphanCollectionCatalogItems()

    // endregion

    // region comments

    @Query("DELETE FROM comment_pages WHERE cachedAt < :minCachedAt")
    abstract suspend fun deleteStaleCommentPages(minCachedAt: Long)

    @Query(
        """
        DELETE FROM comment_items
        WHERE NOT EXISTS (
            SELECT 1 FROM comment_pages AS parent
            WHERE parent.scopeType = comment_items.scopeType
                AND parent.ownerId = comment_items.ownerId
                AND parent.sort = comment_items.sort
                AND parent.`limit` = comment_items.`limit`
                AND parent.skip = comment_items.skip
        )
        """
    )
    abstract suspend fun deleteOrphanCommentItems()

    // endregion

    // region account

    @Query("DELETE FROM account_profiles WHERE cachedAt < :minCachedAt")
    abstract suspend fun deleteStaleAccountProfiles(minCachedAt: Long)

    @Query("DELETE FROM account_anime_list_states WHERE cachedAt < :minCachedAt")
    abstract suspend fun deleteStaleAccountAnimeListStates(minCachedAt: Long)

    @Query("DELETE FROM account_user_ratings WHERE cachedAt < :minCachedAt")
    abstract suspend fun deleteStaleAccountUserRatings(minCachedAt: Long)

    @Query("DELETE FROM account_notification_anime WHERE cachedAt < :minCachedAt")
    abstract suspend fun deleteStaleAccountNotificationAnime(minCachedAt: Long)

    @Query("DELETE FROM account_user_list_pages WHERE cachedAt < :minCachedAt")
    abstract suspend fun deleteStaleAccountUserListPages(minCachedAt: Long)

    @Query(
        """
        DELETE FROM account_user_list_items
        WHERE NOT EXISTS (
            SELECT 1 FROM account_user_list_pages AS parent
            WHERE parent.userId = account_user_list_items.userId
                AND parent.listId = account_user_list_items.listId
                AND parent.language = account_user_list_items.language
        )
        """
    )
    abstract suspend fun deleteOrphanAccountUserListItems()

    @Query("DELETE FROM account_rating_bucket_caches WHERE cachedAt < :minCachedAt")
    abstract suspend fun deleteStaleAccountRatingBucketCaches(minCachedAt: Long)

    @Query(
        """
        DELETE FROM account_rating_buckets
        WHERE NOT EXISTS (
            SELECT 1 FROM account_rating_bucket_caches AS parent
            WHERE parent.animeId = account_rating_buckets.animeId
        )
        """
    )
    abstract suspend fun deleteOrphanAccountRatingBuckets()

    @Query("DELETE FROM account_list_stats_caches WHERE cachedAt < :minCachedAt")
    abstract suspend fun deleteStaleAccountListStatsCaches(minCachedAt: Long)

    @Query(
        """
        DELETE FROM account_list_stats
        WHERE NOT EXISTS (
            SELECT 1 FROM account_list_stats_caches AS parent
            WHERE parent.animeId = account_list_stats.animeId
        )
        """
    )
    abstract suspend fun deleteOrphanAccountListStats()

    @Query("DELETE FROM account_collection_pages WHERE cachedAt < :minCachedAt")
    abstract suspend fun deleteStaleAccountCollectionPages(minCachedAt: Long)

    @Query(
        """
        DELETE FROM account_collection_items
        WHERE NOT EXISTS (
            SELECT 1 FROM account_collection_pages AS parent
            WHERE parent.pageKey = account_collection_items.pageKey
        )
        """
    )
    abstract suspend fun deleteOrphanAccountCollectionItems()

    @Query("DELETE FROM account_video_subscription_caches WHERE cachedAt < :minCachedAt")
    abstract suspend fun deleteStaleAccountVideoSubscriptionCaches(minCachedAt: Long)

    @Query(
        """
        DELETE FROM account_video_subscriptions
        WHERE NOT EXISTS (
            SELECT 1 FROM account_video_subscription_caches AS parent
            WHERE parent.userId = account_video_subscriptions.userId
                AND parent.language = account_video_subscriptions.language
        )
        """
    )
    abstract suspend fun deleteOrphanAccountVideoSubscriptions()

    @Query("DELETE FROM account_notification_pages WHERE cachedAt < :minCachedAt")
    abstract suspend fun deleteStaleAccountNotificationPages(minCachedAt: Long)

    @Query(
        """
        DELETE FROM account_notifications
        WHERE NOT EXISTS (
            SELECT 1 FROM account_notification_pages AS parent
            WHERE parent.userId = account_notifications.userId
                AND parent.language = account_notifications.language
                AND parent.`limit` = account_notifications.`limit`
                AND parent.`offset` = account_notifications.`offset`
        )
        """
    )
    abstract suspend fun deleteOrphanAccountNotifications()

    @Query("DELETE FROM account_notification_count_caches WHERE cachedAt < :minCachedAt")
    abstract suspend fun deleteStaleAccountNotificationCountCaches(minCachedAt: Long)

    @Query(
        """
        DELETE FROM account_notification_counts
        WHERE NOT EXISTS (
            SELECT 1 FROM account_notification_count_caches AS parent
            WHERE parent.userId = account_notification_counts.userId
        )
        """
    )
    abstract suspend fun deleteOrphanAccountNotificationCounts()

    @Query("DELETE FROM account_user_profile_content_pages WHERE cachedAt < :minCachedAt")
    abstract suspend fun deleteStaleAccountUserProfileContentPages(minCachedAt: Long)

    @Query(
        """
        DELETE FROM account_user_friends
        WHERE NOT EXISTS (
            SELECT 1 FROM account_user_profile_content_pages AS parent
            WHERE parent.userId = account_user_friends.userId
                AND parent.language = account_user_friends.language
                AND parent.`limit` = account_user_friends.`limit`
                AND parent.`offset` = account_user_friends.`offset`
                AND parent.contentType = 'friends'
        )
        """
    )
    abstract suspend fun deleteOrphanAccountUserFriends()

    @Query(
        """
        DELETE FROM account_user_reviews
        WHERE NOT EXISTS (
            SELECT 1 FROM account_user_profile_content_pages AS parent
            WHERE parent.userId = account_user_reviews.userId
                AND parent.language = account_user_reviews.language
                AND parent.`limit` = account_user_reviews.`limit`
                AND parent.`offset` = account_user_reviews.`offset`
                AND parent.contentType = 'reviews'
        )
        """
    )
    abstract suspend fun deleteOrphanAccountUserReviews()

    @Query(
        """
        DELETE FROM account_user_posts
        WHERE NOT EXISTS (
            SELECT 1 FROM account_user_profile_content_pages AS parent
            WHERE parent.userId = account_user_posts.userId
                AND parent.language = account_user_posts.language
                AND parent.`limit` = account_user_posts.`limit`
                AND parent.`offset` = account_user_posts.`offset`
                AND parent.contentType = 'posts'
        )
        """
    )
    abstract suspend fun deleteOrphanAccountUserPosts()

    @Query("DELETE FROM account_user_stats_caches WHERE cachedAt < :minCachedAt")
    abstract suspend fun deleteStaleAccountUserStatsCaches(minCachedAt: Long)

    @Query(
        """
        DELETE FROM account_user_genre_stats
        WHERE NOT EXISTS (
            SELECT 1 FROM account_user_stats_caches AS parent
            WHERE parent.userId = account_user_genre_stats.userId
                AND parent.language = account_user_genre_stats.language
        )
        """
    )
    abstract suspend fun deleteOrphanAccountUserGenreStats()

    @Query(
        """
        DELETE FROM account_user_rating_stats
        WHERE NOT EXISTS (
            SELECT 1 FROM account_user_stats_caches AS parent
            WHERE parent.userId = account_user_rating_stats.userId
                AND parent.language = account_user_rating_stats.language
        )
        """
    )
    abstract suspend fun deleteOrphanAccountUserRatingStats()

    @Query(
        """
        DELETE FROM account_user_list_watch_stats
        WHERE NOT EXISTS (
            SELECT 1 FROM account_user_stats_caches AS parent
            WHERE parent.userId = account_user_list_watch_stats.userId
                AND parent.language = account_user_list_watch_stats.language
        )
        """
    )
    abstract suspend fun deleteOrphanAccountUserListWatchStats()

    @Query(
        """
        DELETE FROM account_user_type_stats
        WHERE NOT EXISTS (
            SELECT 1 FROM account_user_stats_caches AS parent
            WHERE parent.userId = account_user_type_stats.userId
                AND parent.language = account_user_type_stats.language
        )
        """
    )
    abstract suspend fun deleteOrphanAccountUserTypeStats()

    @Query("DELETE FROM account_user_profile_summary_caches WHERE cachedAt < :minCachedAt")
    abstract suspend fun deleteStaleAccountUserProfileSummaryCaches(minCachedAt: Long)

    @Query(
        """
        DELETE FROM account_user_profile_watch_types
        WHERE NOT EXISTS (
            SELECT 1 FROM account_user_profile_summary_caches AS parent
            WHERE parent.userId = account_user_profile_watch_types.userId
                AND parent.language = account_user_profile_watch_types.language
        )
        """
    )
    abstract suspend fun deleteOrphanAccountUserProfileWatchTypes()

    @Query(
        """
        DELETE FROM account_user_profile_watch_history
        WHERE NOT EXISTS (
            SELECT 1 FROM account_user_profile_summary_caches AS parent
            WHERE parent.userId = account_user_profile_watch_history.userId
                AND parent.language = account_user_profile_watch_history.language
        )
        """
    )
    abstract suspend fun deleteOrphanAccountUserProfileWatchHistory()

    // endregion
}
