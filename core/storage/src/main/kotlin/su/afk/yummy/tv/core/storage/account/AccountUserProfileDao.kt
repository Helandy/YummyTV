package su.afk.yummy.tv.core.storage.account

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction

@Dao
abstract class AccountUserProfileDao {

    @Query(
        """
        SELECT * FROM account_user_profile_content_pages
        WHERE userId = :userId
            AND language = :language
            AND contentType = :contentType
            AND `limit` = :limit
            AND `offset` = :offset
        LIMIT 1
        """
    )
    abstract suspend fun getUserProfileContentPageEntry(
        userId: Int,
        language: String,
        contentType: String,
        limit: Int,
        offset: Int,
    ): AccountUserProfileContentPageEntry?

    @Query(
        """
        SELECT * FROM account_user_friends
        WHERE userId = :userId AND language = :language AND `limit` = :limit AND `offset` = :offset
        ORDER BY position
        """
    )
    abstract suspend fun getUserFriendEntries(
        userId: Int,
        language: String,
        limit: Int,
        offset: Int,
    ): List<AccountUserFriendEntry>

    @Query(
        """
        SELECT * FROM account_user_reviews
        WHERE userId = :userId AND language = :language AND `limit` = :limit AND `offset` = :offset
        ORDER BY position
        """
    )
    abstract suspend fun getUserReviewEntries(
        userId: Int,
        language: String,
        limit: Int,
        offset: Int,
    ): List<AccountUserReviewEntry>

    @Query(
        """
        SELECT * FROM account_user_posts
        WHERE userId = :userId AND language = :language AND `limit` = :limit AND `offset` = :offset
        ORDER BY position
        """
    )
    abstract suspend fun getUserPostEntries(
        userId: Int,
        language: String,
        limit: Int,
        offset: Int,
    ): List<AccountUserPostEntry>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertUserProfileContentPage(entry: AccountUserProfileContentPageEntry)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertUserFriends(entries: List<AccountUserFriendEntry>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertUserReviews(entries: List<AccountUserReviewEntry>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertUserPosts(entries: List<AccountUserPostEntry>)

    @Query("DELETE FROM account_user_profile_content_pages WHERE userId = :userId")
    abstract suspend fun deleteUserProfileContentPages(userId: Int)

    @Query(
        "DELETE FROM account_user_profile_content_pages " +
                "WHERE userId = :userId AND contentType = :contentType"
    )
    abstract suspend fun deleteUserProfileContentPages(userId: Int, contentType: String)

    @Query("DELETE FROM account_user_friends WHERE userId = :userId")
    abstract suspend fun deleteUserFriends(userId: Int)

    @Query("DELETE FROM account_user_reviews WHERE userId = :userId")
    abstract suspend fun deleteUserReviews(userId: Int)

    @Query("DELETE FROM account_user_posts WHERE userId = :userId")
    abstract suspend fun deleteUserPosts(userId: Int)

    @Query(
        """
        DELETE FROM account_user_profile_content_pages
        WHERE userId = :userId
            AND language = :language
            AND contentType = :contentType
            AND `limit` = :limit
            AND `offset` = :offset
        """
    )
    abstract suspend fun deleteUserProfileContentPage(
        userId: Int,
        language: String,
        contentType: String,
        limit: Int,
        offset: Int,
    )

    @Query(
        """
        DELETE FROM account_user_friends
        WHERE userId = :userId AND language = :language AND `limit` = :limit AND `offset` = :offset
        """
    )
    abstract suspend fun deleteUserFriendItems(
        userId: Int,
        language: String,
        limit: Int,
        offset: Int,
    )

    @Query(
        """
        DELETE FROM account_user_reviews
        WHERE userId = :userId AND language = :language AND `limit` = :limit AND `offset` = :offset
        """
    )
    abstract suspend fun deleteUserReviewItems(
        userId: Int,
        language: String,
        limit: Int,
        offset: Int,
    )

    @Query(
        """
        DELETE FROM account_user_posts
        WHERE userId = :userId AND language = :language AND `limit` = :limit AND `offset` = :offset
        """
    )
    abstract suspend fun deleteUserPostItems(
        userId: Int,
        language: String,
        limit: Int,
        offset: Int,
    )

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

    @Query(
        """
        SELECT * FROM account_user_profile_summary_caches
        WHERE userId = :userId AND language = :language
        LIMIT 1
        """
    )
    abstract suspend fun getUserProfileSummaryCacheEntry(
        userId: Int,
        language: String,
    ): AccountUserProfileSummaryCacheEntry?

    @Query(
        """
        SELECT * FROM account_user_profile_watch_types
        WHERE userId = :userId AND language = :language
        ORDER BY position
        """
    )
    abstract suspend fun getUserProfileWatchTypes(
        userId: Int,
        language: String,
    ): List<AccountUserProfileWatchTypeEntry>

    @Query(
        """
        SELECT * FROM account_user_profile_watch_history
        WHERE userId = :userId AND language = :language
        ORDER BY position
        """
    )
    abstract suspend fun getUserProfileWatchHistory(
        userId: Int,
        language: String,
    ): List<AccountUserProfileWatchHistoryEntry>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertUserProfileSummaryCache(entry: AccountUserProfileSummaryCacheEntry)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertUserProfileWatchTypes(entries: List<AccountUserProfileWatchTypeEntry>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertUserProfileWatchHistory(entries: List<AccountUserProfileWatchHistoryEntry>)

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

    @Query("DELETE FROM account_user_profile_summary_caches WHERE userId = :userId")
    abstract suspend fun deleteUserProfileSummaryCaches(userId: Int)

    @Query("DELETE FROM account_user_profile_watch_types WHERE userId = :userId")
    abstract suspend fun deleteUserProfileWatchTypes(userId: Int)

    @Query("DELETE FROM account_user_profile_watch_history WHERE userId = :userId")
    abstract suspend fun deleteUserProfileWatchHistory(userId: Int)

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

    @Query(
        """
        DELETE FROM account_user_profile_summary_caches
        WHERE userId = :userId AND language = :language
        """
    )
    abstract suspend fun deleteUserProfileSummaryCache(userId: Int, language: String)

    @Query(
        """
        DELETE FROM account_user_profile_watch_types
        WHERE userId = :userId AND language = :language
        """
    )
    abstract suspend fun deleteUserProfileWatchTypes(userId: Int, language: String)

    @Query(
        """
        DELETE FROM account_user_profile_watch_history
        WHERE userId = :userId AND language = :language
        """
    )
    abstract suspend fun deleteUserProfileWatchHistory(userId: Int, language: String)

    @Transaction
    open suspend fun getUserFriendsPage(
        userId: Int,
        language: String,
        limit: Int,
        offset: Int,
    ): AccountUserFriendsPageCache? {
        val entry = getUserProfileContentPageEntry(
            userId = userId,
            language = language,
            contentType = ACCOUNT_USER_PROFILE_CONTENT_FRIENDS,
            limit = limit,
            offset = offset,
        ) ?: return null
        return AccountUserFriendsPageCache(
            entry,
            getUserFriendEntries(userId, language, limit, offset),
        )
    }

    @Transaction
    open suspend fun replaceUserFriendsPage(cache: AccountUserFriendsPageCache) {
        val entry = cache.entry
        deleteUserProfileContentPage(
            entry.userId,
            entry.language,
            entry.contentType,
            entry.limit,
            entry.offset,
        )
        deleteUserFriendItems(entry.userId, entry.language, entry.limit, entry.offset)
        insertUserProfileContentPage(entry)
        if (cache.items.isNotEmpty()) insertUserFriends(cache.items)
    }

    @Transaction
    open suspend fun getUserReviewsPage(
        userId: Int,
        language: String,
        limit: Int,
        offset: Int,
    ): AccountUserReviewsPageCache? {
        val entry = getUserProfileContentPageEntry(
            userId = userId,
            language = language,
            contentType = ACCOUNT_USER_PROFILE_CONTENT_REVIEWS,
            limit = limit,
            offset = offset,
        ) ?: return null
        return AccountUserReviewsPageCache(
            entry,
            getUserReviewEntries(userId, language, limit, offset),
        )
    }

    @Transaction
    open suspend fun replaceUserReviewsPage(cache: AccountUserReviewsPageCache) {
        val entry = cache.entry
        deleteUserProfileContentPage(
            entry.userId,
            entry.language,
            entry.contentType,
            entry.limit,
            entry.offset,
        )
        deleteUserReviewItems(entry.userId, entry.language, entry.limit, entry.offset)
        insertUserProfileContentPage(entry)
        if (cache.items.isNotEmpty()) insertUserReviews(cache.items)
    }

    @Transaction
    open suspend fun getUserPostsPage(
        userId: Int,
        language: String,
        limit: Int,
        offset: Int,
    ): AccountUserPostsPageCache? {
        val entry = getUserProfileContentPageEntry(
            userId = userId,
            language = language,
            contentType = ACCOUNT_USER_PROFILE_CONTENT_POSTS,
            limit = limit,
            offset = offset,
        ) ?: return null
        return AccountUserPostsPageCache(
            entry,
            getUserPostEntries(userId, language, limit, offset),
        )
    }

    @Transaction
    open suspend fun replaceUserPostsPage(cache: AccountUserPostsPageCache) {
        val entry = cache.entry
        deleteUserProfileContentPage(
            entry.userId,
            entry.language,
            entry.contentType,
            entry.limit,
            entry.offset,
        )
        deleteUserPostItems(entry.userId, entry.language, entry.limit, entry.offset)
        insertUserProfileContentPage(entry)
        if (cache.items.isNotEmpty()) insertUserPosts(cache.items)
    }

    @Transaction
    open suspend fun deleteUserProfileContentForUser(userId: Int) {
        deleteUserProfileContentPages(userId)
        deleteUserFriends(userId)
        deleteUserReviews(userId)
        deleteUserPosts(userId)
    }

    @Transaction
    open suspend fun deleteUserFriendsContentForUser(userId: Int) {
        deleteUserProfileContentPages(userId, ACCOUNT_USER_PROFILE_CONTENT_FRIENDS)
        deleteUserFriends(userId)
    }

    @Transaction
    open suspend fun deleteUserProfileSummaryForUser(userId: Int) {
        deleteUserProfileSummaryCaches(userId)
        deleteUserProfileWatchTypes(userId)
        deleteUserProfileWatchHistory(userId)
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
    open suspend fun getUserProfileSummary(
        userId: Int,
        language: String,
    ): AccountUserProfileSummaryCache? {
        val entry = getUserProfileSummaryCacheEntry(userId, language) ?: return null
        return AccountUserProfileSummaryCache(
            entry = entry,
            watchTypes = getUserProfileWatchTypes(userId, language),
            watchHistory = getUserProfileWatchHistory(userId, language),
        )
    }

    @Transaction
    open suspend fun replaceUserProfileSummary(cache: AccountUserProfileSummaryCache) {
        val userId = cache.entry.userId
        val language = cache.entry.language
        deleteUserProfileSummaryCache(userId, language)
        deleteUserProfileWatchTypes(userId, language)
        deleteUserProfileWatchHistory(userId, language)
        insertUserProfileSummaryCache(cache.entry)
        if (cache.watchTypes.isNotEmpty()) insertUserProfileWatchTypes(cache.watchTypes)
        if (cache.watchHistory.isNotEmpty()) insertUserProfileWatchHistory(cache.watchHistory)
    }

    @Transaction
    open suspend fun deleteUserStatsForUser(userId: Int) {
        deleteUserStatsCaches(userId)
        deleteUserGenreStats(userId)
        deleteUserRatingStats(userId)
        deleteUserListWatchStats(userId)
        deleteUserTypeStats(userId)
        deleteUserProfileSummaryCaches(userId)
        deleteUserProfileWatchTypes(userId)
        deleteUserProfileWatchHistory(userId)
    }
}
