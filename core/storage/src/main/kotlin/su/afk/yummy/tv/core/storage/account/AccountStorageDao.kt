package su.afk.yummy.tv.core.storage.account

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction

@Dao
abstract class AccountStorageDao {

    @Query("SELECT * FROM account_profiles WHERE profileKey = :profileKey LIMIT 1")
    abstract suspend fun getProfile(profileKey: String): AccountProfileEntry?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertProfile(entry: AccountProfileEntry)

    @Query("DELETE FROM account_profiles WHERE profileKey = :profileKey")
    abstract suspend fun deleteProfile(profileKey: String)

    @Query("DELETE FROM account_profiles WHERE userId = :userId")
    abstract suspend fun deleteProfilesByUser(userId: Int)

    @Query(
        """
        SELECT * FROM account_user_list_pages
        WHERE userId = :userId AND listId = :listId AND language = :language
        LIMIT 1
        """
    )
    abstract suspend fun getUserListPageEntry(
        userId: Int,
        listId: Int,
        language: String,
    ): AccountUserListPageEntry?

    @Query(
        """
        SELECT * FROM account_user_list_items
        WHERE userId = :userId AND listId = :listId AND language = :language
        ORDER BY position
        """
    )
    abstract suspend fun getUserListItems(
        userId: Int,
        listId: Int,
        language: String,
    ): List<AccountUserListItemEntry>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertUserListPage(entry: AccountUserListPageEntry)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertUserListItems(entries: List<AccountUserListItemEntry>)

    @Query("DELETE FROM account_user_list_pages WHERE userId = :userId")
    abstract suspend fun deleteUserListPages(userId: Int)

    @Query("DELETE FROM account_user_list_items WHERE userId = :userId")
    abstract suspend fun deleteUserListItems(userId: Int)

    @Query(
        """
        DELETE FROM account_user_list_pages
        WHERE userId = :userId AND listId = :listId AND language = :language
        """
    )
    abstract suspend fun deleteUserListPage(userId: Int, listId: Int, language: String)

    @Query(
        """
        DELETE FROM account_user_list_items
        WHERE userId = :userId AND listId = :listId AND language = :language
        """
    )
    abstract suspend fun deleteUserListPageItems(userId: Int, listId: Int, language: String)

    @Query("SELECT * FROM account_anime_list_states WHERE userId = :userId AND animeId = :animeId LIMIT 1")
    abstract suspend fun getAnimeListState(userId: Int, animeId: Int): AccountAnimeListStateEntry?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertAnimeListState(entry: AccountAnimeListStateEntry)

    @Query("DELETE FROM account_anime_list_states WHERE userId = :userId")
    abstract suspend fun deleteAnimeListStates(userId: Int)

    @Query("SELECT * FROM account_rating_bucket_caches WHERE animeId = :animeId LIMIT 1")
    abstract suspend fun getRatingBucketCacheEntry(animeId: Int): AccountRatingBucketCacheEntry?

    @Query("SELECT * FROM account_rating_buckets WHERE animeId = :animeId ORDER BY position")
    abstract suspend fun getRatingBucketEntries(animeId: Int): List<AccountRatingBucketEntry>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertRatingBucketCache(entry: AccountRatingBucketCacheEntry)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertRatingBuckets(entries: List<AccountRatingBucketEntry>)

    @Query("DELETE FROM account_rating_bucket_caches WHERE animeId = :animeId")
    abstract suspend fun deleteRatingBucketCache(animeId: Int)

    @Query("DELETE FROM account_rating_buckets WHERE animeId = :animeId")
    abstract suspend fun deleteRatingBuckets(animeId: Int)

    @Query("SELECT * FROM account_user_ratings WHERE userId = :userId AND animeId = :animeId LIMIT 1")
    abstract suspend fun getUserRating(userId: Int, animeId: Int): AccountUserRatingEntry?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertUserRating(entry: AccountUserRatingEntry)

    @Query("DELETE FROM account_user_ratings WHERE userId = :userId")
    abstract suspend fun deleteUserRatings(userId: Int)

    @Query("SELECT * FROM account_list_stats_caches WHERE animeId = :animeId LIMIT 1")
    abstract suspend fun getListStatsCacheEntry(animeId: Int): AccountListStatsCacheEntry?

    @Query("SELECT * FROM account_list_stats WHERE animeId = :animeId")
    abstract suspend fun getListStatsEntries(animeId: Int): List<AccountListStatEntry>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertListStatsCache(entry: AccountListStatsCacheEntry)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertListStats(entries: List<AccountListStatEntry>)

    @Query("DELETE FROM account_list_stats_caches WHERE animeId = :animeId")
    abstract suspend fun deleteListStatsCache(animeId: Int)

    @Query("DELETE FROM account_list_stats WHERE animeId = :animeId")
    abstract suspend fun deleteListStats(animeId: Int)

    @Query("SELECT * FROM account_collection_pages WHERE pageKey = :pageKey LIMIT 1")
    abstract suspend fun getCollectionPageEntry(pageKey: String): AccountCollectionPageEntry?

    @Query("SELECT * FROM account_collection_items WHERE pageKey = :pageKey ORDER BY position")
    abstract suspend fun getCollectionItems(pageKey: String): List<AccountCollectionItemEntry>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertCollectionPage(entry: AccountCollectionPageEntry)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertCollectionItems(entries: List<AccountCollectionItemEntry>)

    @Query("DELETE FROM account_collection_pages WHERE pageKey = :pageKey")
    abstract suspend fun deleteCollectionPage(pageKey: String)

    @Query("DELETE FROM account_collection_items WHERE pageKey = :pageKey")
    abstract suspend fun deleteCollectionItems(pageKey: String)

    @Query(
        """
        SELECT * FROM account_video_subscription_caches
        WHERE userId = :userId AND language = :language
        LIMIT 1
        """
    )
    abstract suspend fun getVideoSubscriptionCacheEntry(
        userId: Int,
        language: String,
    ): AccountVideoSubscriptionCacheEntry?

    @Query(
        """
        SELECT * FROM account_video_subscriptions
        WHERE userId = :userId AND language = :language
        ORDER BY position
        """
    )
    abstract suspend fun getVideoSubscriptionEntries(
        userId: Int,
        language: String
    ): List<AccountVideoSubscriptionEntry>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertVideoSubscriptionCache(entry: AccountVideoSubscriptionCacheEntry)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertVideoSubscriptions(entries: List<AccountVideoSubscriptionEntry>)

    @Query("DELETE FROM account_video_subscription_caches WHERE userId = :userId")
    abstract suspend fun deleteVideoSubscriptionCaches(userId: Int)

    @Query("DELETE FROM account_video_subscriptions WHERE userId = :userId")
    abstract suspend fun deleteVideoSubscriptions(userId: Int)

    @Query(
        """
        DELETE FROM account_video_subscription_caches
        WHERE userId = :userId AND language = :language
        """
    )
    abstract suspend fun deleteVideoSubscriptionCache(userId: Int, language: String)

    @Query(
        """
        DELETE FROM account_video_subscriptions
        WHERE userId = :userId AND language = :language
        """
    )
    abstract suspend fun deleteVideoSubscriptionItems(userId: Int, language: String)

    @Query(
        """
        SELECT * FROM account_notification_pages
        WHERE userId = :userId AND language = :language AND `limit` = :limit AND `offset` = :offset
        LIMIT 1
        """
    )
    abstract suspend fun getNotificationPageEntry(
        userId: Int,
        language: String,
        limit: Int,
        offset: Int,
    ): AccountNotificationPageEntry?

    @Query(
        """
        SELECT * FROM account_notifications
        WHERE userId = :userId AND language = :language AND `limit` = :limit AND `offset` = :offset
        ORDER BY position
        """
    )
    abstract suspend fun getNotificationEntries(
        userId: Int,
        language: String,
        limit: Int,
        offset: Int,
    ): List<AccountNotificationEntry>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertNotificationPage(entry: AccountNotificationPageEntry)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertNotifications(entries: List<AccountNotificationEntry>)

    @Query("DELETE FROM account_notification_pages WHERE userId = :userId")
    abstract suspend fun deleteNotificationPages(userId: Int)

    @Query("DELETE FROM account_notifications WHERE userId = :userId")
    abstract suspend fun deleteNotifications(userId: Int)

    @Query(
        """
        DELETE FROM account_notification_pages
        WHERE userId = :userId AND language = :language AND `limit` = :limit AND `offset` = :offset
        """
    )
    abstract suspend fun deleteNotificationPage(
        userId: Int,
        language: String,
        limit: Int,
        offset: Int,
    )

    @Query(
        """
        DELETE FROM account_notifications
        WHERE userId = :userId AND language = :language AND `limit` = :limit AND `offset` = :offset
        """
    )
    abstract suspend fun deleteNotificationItems(
        userId: Int,
        language: String,
        limit: Int,
        offset: Int,
    )

    @Query("SELECT * FROM account_notification_count_caches WHERE userId = :userId LIMIT 1")
    abstract suspend fun getNotificationCountCacheEntry(userId: Int): AccountNotificationCountCacheEntry?

    @Query("SELECT * FROM account_notification_counts WHERE userId = :userId ORDER BY position")
    abstract suspend fun getNotificationCountEntries(userId: Int): List<AccountNotificationCountEntry>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertNotificationCountCache(entry: AccountNotificationCountCacheEntry)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertNotificationCounts(entries: List<AccountNotificationCountEntry>)

    @Query("DELETE FROM account_notification_count_caches WHERE userId = :userId")
    abstract suspend fun deleteNotificationCountCache(userId: Int)

    @Query("DELETE FROM account_notification_counts WHERE userId = :userId")
    abstract suspend fun deleteNotificationCounts(userId: Int)

    @Query("SELECT * FROM account_notification_anime WHERE slug = :slug LIMIT 1")
    abstract suspend fun getNotificationAnime(slug: String): AccountNotificationAnimeEntry?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertNotificationAnime(entry: AccountNotificationAnimeEntry)

    @Query(
        """
        SELECT * FROM account_user_stats_caches
        WHERE userId = :userId AND language = :language
        LIMIT 1
        """
    )
    abstract suspend fun getUserStatsCacheEntry(
        userId: Int,
        language: String
    ): AccountUserStatsCacheEntry?

    @Query("SELECT * FROM account_user_genre_stats WHERE userId = :userId AND language = :language ORDER BY position")
    abstract suspend fun getUserGenreStats(
        userId: Int,
        language: String
    ): List<AccountUserGenreStatEntry>

    @Query("SELECT * FROM account_user_rating_stats WHERE userId = :userId AND language = :language ORDER BY position")
    abstract suspend fun getUserRatingStats(
        userId: Int,
        language: String
    ): List<AccountUserRatingStatEntry>

    @Query(
        """
        SELECT * FROM account_user_list_watch_stats
        WHERE userId = :userId AND language = :language
        ORDER BY position
        """
    )
    abstract suspend fun getUserListWatchStats(
        userId: Int,
        language: String
    ): List<AccountUserListWatchStatEntry>

    @Query("SELECT * FROM account_user_type_stats WHERE userId = :userId AND language = :language ORDER BY position")
    abstract suspend fun getUserTypeStats(
        userId: Int,
        language: String
    ): List<AccountUserTypeStatEntry>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertUserStatsCache(entry: AccountUserStatsCacheEntry)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertUserGenreStats(entries: List<AccountUserGenreStatEntry>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertUserRatingStats(entries: List<AccountUserRatingStatEntry>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertUserListWatchStats(entries: List<AccountUserListWatchStatEntry>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertUserTypeStats(entries: List<AccountUserTypeStatEntry>)

    @Query("DELETE FROM account_user_stats_caches WHERE userId = :userId")
    abstract suspend fun deleteUserStatsCaches(userId: Int)

    @Query("DELETE FROM account_user_genre_stats WHERE userId = :userId")
    abstract suspend fun deleteUserGenreStats(userId: Int)

    @Query("DELETE FROM account_user_rating_stats WHERE userId = :userId")
    abstract suspend fun deleteUserRatingStats(userId: Int)

    @Query("DELETE FROM account_user_list_watch_stats WHERE userId = :userId")
    abstract suspend fun deleteUserListWatchStats(userId: Int)

    @Query("DELETE FROM account_user_type_stats WHERE userId = :userId")
    abstract suspend fun deleteUserTypeStats(userId: Int)

    @Query(
        """
        DELETE FROM account_user_stats_caches
        WHERE userId = :userId AND language = :language
        """
    )
    abstract suspend fun deleteUserStatsCache(userId: Int, language: String)

    @Query("DELETE FROM account_user_genre_stats WHERE userId = :userId AND language = :language")
    abstract suspend fun deleteUserGenreStats(userId: Int, language: String)

    @Query("DELETE FROM account_user_rating_stats WHERE userId = :userId AND language = :language")
    abstract suspend fun deleteUserRatingStats(userId: Int, language: String)

    @Query("DELETE FROM account_user_list_watch_stats WHERE userId = :userId AND language = :language")
    abstract suspend fun deleteUserListWatchStats(userId: Int, language: String)

    @Query("DELETE FROM account_user_type_stats WHERE userId = :userId AND language = :language")
    abstract suspend fun deleteUserTypeStats(userId: Int, language: String)

    @Transaction
    open suspend fun getUserList(
        userId: Int,
        listId: Int,
        language: String,
    ): AccountUserListCache? {
        val entry = getUserListPageEntry(userId, listId, language) ?: return null
        return AccountUserListCache(entry, getUserListItems(userId, listId, language))
    }

    @Transaction
    open suspend fun replaceUserList(cache: AccountUserListCache) {
        val entry = cache.entry
        deleteUserListPage(entry.userId, entry.listId, entry.language)
        deleteUserListPageItems(entry.userId, entry.listId, entry.language)
        insertUserListPage(entry)
        if (cache.items.isNotEmpty()) insertUserListItems(cache.items)
    }

    @Transaction
    open suspend fun deleteUserLists(userId: Int) {
        deleteUserListPages(userId)
        deleteUserListItems(userId)
    }

    @Transaction
    open suspend fun getRatingBuckets(animeId: Int): AccountRatingBucketsCache? {
        val entry = getRatingBucketCacheEntry(animeId) ?: return null
        return AccountRatingBucketsCache(entry, getRatingBucketEntries(animeId))
    }

    @Transaction
    open suspend fun replaceRatingBuckets(cache: AccountRatingBucketsCache) {
        val animeId = cache.entry.animeId
        deleteRatingBucketCache(animeId)
        deleteRatingBuckets(animeId)
        insertRatingBucketCache(cache.entry)
        if (cache.buckets.isNotEmpty()) insertRatingBuckets(cache.buckets)
    }

    @Transaction
    open suspend fun deleteRatingBucketsCache(animeId: Int) {
        deleteRatingBucketCache(animeId)
        deleteRatingBuckets(animeId)
    }

    @Transaction
    open suspend fun getListStats(animeId: Int): AccountListStatsCache? {
        val entry = getListStatsCacheEntry(animeId) ?: return null
        return AccountListStatsCache(entry, getListStatsEntries(animeId))
    }

    @Transaction
    open suspend fun replaceListStats(cache: AccountListStatsCache) {
        val animeId = cache.entry.animeId
        deleteListStatsCache(animeId)
        deleteListStats(animeId)
        insertListStatsCache(cache.entry)
        if (cache.stats.isNotEmpty()) insertListStats(cache.stats)
    }

    @Transaction
    open suspend fun getCollections(pageKey: String): AccountCollectionsPageCache? {
        val entry = getCollectionPageEntry(pageKey) ?: return null
        return AccountCollectionsPageCache(entry, getCollectionItems(pageKey))
    }

    @Transaction
    open suspend fun replaceCollections(cache: AccountCollectionsPageCache) {
        val pageKey = cache.entry.pageKey
        deleteCollectionPage(pageKey)
        deleteCollectionItems(pageKey)
        insertCollectionPage(cache.entry)
        if (cache.items.isNotEmpty()) insertCollectionItems(cache.items)
    }

    @Transaction
    open suspend fun getVideoSubscriptions(
        userId: Int,
        language: String,
    ): AccountVideoSubscriptionsCache? {
        val entry = getVideoSubscriptionCacheEntry(userId, language) ?: return null
        return AccountVideoSubscriptionsCache(entry, getVideoSubscriptionEntries(userId, language))
    }

    @Transaction
    open suspend fun replaceVideoSubscriptions(cache: AccountVideoSubscriptionsCache) {
        val entry = cache.entry
        deleteVideoSubscriptionCache(entry.userId, entry.language)
        deleteVideoSubscriptionItems(entry.userId, entry.language)
        insertVideoSubscriptionCache(entry)
        if (cache.items.isNotEmpty()) insertVideoSubscriptions(cache.items)
    }

    @Transaction
    open suspend fun deleteVideoSubscriptionsForUser(userId: Int) {
        deleteVideoSubscriptionCaches(userId)
        deleteVideoSubscriptions(userId)
    }

    @Transaction
    open suspend fun getNotifications(
        userId: Int,
        language: String,
        limit: Int,
        offset: Int,
    ): AccountNotificationsPageCache? {
        val entry = getNotificationPageEntry(userId, language, limit, offset) ?: return null
        return AccountNotificationsPageCache(
            entry,
            getNotificationEntries(userId, language, limit, offset)
        )
    }

    @Transaction
    open suspend fun replaceNotifications(cache: AccountNotificationsPageCache) {
        val entry = cache.entry
        deleteNotificationPage(entry.userId, entry.language, entry.limit, entry.offset)
        deleteNotificationItems(entry.userId, entry.language, entry.limit, entry.offset)
        insertNotificationPage(entry)
        if (cache.items.isNotEmpty()) insertNotifications(cache.items)
    }

    @Transaction
    open suspend fun deleteNotificationsForUser(userId: Int) {
        deleteNotificationPages(userId)
        deleteNotifications(userId)
    }

    @Transaction
    open suspend fun getNotificationCounts(userId: Int): AccountNotificationCountsCache? {
        val entry = getNotificationCountCacheEntry(userId) ?: return null
        return AccountNotificationCountsCache(entry, getNotificationCountEntries(userId))
    }

    @Transaction
    open suspend fun replaceNotificationCounts(cache: AccountNotificationCountsCache) {
        val userId = cache.entry.userId
        deleteNotificationCountCache(userId)
        deleteNotificationCounts(userId)
        insertNotificationCountCache(cache.entry)
        if (cache.items.isNotEmpty()) insertNotificationCounts(cache.items)
    }

    @Transaction
    open suspend fun getUserStats(userId: Int, language: String): AccountUserStatsCache? {
        val entry = getUserStatsCacheEntry(userId, language) ?: return null
        return AccountUserStatsCache(
            entry = entry,
            genres = getUserGenreStats(userId, language),
            ratings = getUserRatingStats(userId, language),
            lists = getUserListWatchStats(userId, language),
            types = getUserTypeStats(userId, language),
        )
    }

    @Transaction
    open suspend fun replaceUserStats(cache: AccountUserStatsCache) {
        val userId = cache.entry.userId
        val language = cache.entry.language
        deleteUserStatsCache(userId, language)
        deleteUserGenreStats(userId, language)
        deleteUserRatingStats(userId, language)
        deleteUserListWatchStats(userId, language)
        deleteUserTypeStats(userId, language)
        insertUserStatsCache(cache.entry)
        if (cache.genres.isNotEmpty()) insertUserGenreStats(cache.genres)
        if (cache.ratings.isNotEmpty()) insertUserRatingStats(cache.ratings)
        if (cache.lists.isNotEmpty()) insertUserListWatchStats(cache.lists)
        if (cache.types.isNotEmpty()) insertUserTypeStats(cache.types)
    }

    @Transaction
    open suspend fun deleteUserStatsForUser(userId: Int) {
        deleteUserStatsCaches(userId)
        deleteUserGenreStats(userId)
        deleteUserRatingStats(userId)
        deleteUserListWatchStats(userId)
        deleteUserTypeStats(userId)
    }

    @Transaction
    open suspend fun clearUserScoped(userId: Int) {
        deleteProfile(ACCOUNT_PROFILE_KEY_CURRENT)
        deleteProfilesByUser(userId)
        deleteUserLists(userId)
        deleteAnimeListStates(userId)
        deleteUserRatings(userId)
        deleteVideoSubscriptionsForUser(userId)
        deleteNotificationsForUser(userId)
        deleteNotificationCountCache(userId)
        deleteNotificationCounts(userId)
        deleteUserStatsForUser(userId)
    }
}
