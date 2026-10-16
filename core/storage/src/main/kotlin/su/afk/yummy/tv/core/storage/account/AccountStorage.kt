package su.afk.yummy.tv.core.storage.account

import kotlinx.coroutines.flow.Flow

/** Абстракция над локальным кэшем аккаунта (профиль, списки, рейтинги, уведомления и т.д.) — позволяет подменять реализацию в тестах. */
interface AccountStorage {

    suspend fun getProfile(profileKey: String): AccountProfileEntry?

    suspend fun saveProfile(entry: AccountProfileEntry)

    suspend fun deleteProfile(profileKey: String)

    suspend fun getUserList(
        userId: Int,
        listId: Int,
        language: String,
    ): AccountUserListCache?

    suspend fun saveUserList(cache: AccountUserListCache)

    suspend fun saveUserLists(caches: List<AccountUserListCache>)

    suspend fun hasUserListCache(userId: Int): Boolean

    suspend fun deleteUserLists(userId: Int)

    suspend fun getAnimeListState(userId: Int, animeId: Int): AccountAnimeListStateEntry?

    suspend fun saveAnimeListState(entry: AccountAnimeListStateEntry)

    suspend fun getRatingBuckets(animeId: Int): AccountRatingBucketsCache?

    suspend fun saveRatingBuckets(cache: AccountRatingBucketsCache)

    suspend fun deleteRatingBuckets(animeId: Int)

    suspend fun getUserRating(userId: Int, animeId: Int): AccountUserRatingEntry?

    fun observeUserRating(userId: Int, animeId: Int): Flow<AccountUserRatingEntry?>

    suspend fun saveUserRating(entry: AccountUserRatingEntry)

    suspend fun getListStats(animeId: Int): AccountListStatsCache?

    suspend fun saveListStats(cache: AccountListStatsCache)

    suspend fun invalidateListStats(animeId: Int)

    suspend fun getCollections(pageKey: String): AccountCollectionsPageCache?

    suspend fun saveCollections(
        cache: AccountCollectionsPageCache,
        prunePagesCachedBefore: Long? = null,
    )

    suspend fun invalidateCollections()

    suspend fun getVideoSubscriptions(
        userId: Int,
        language: String,
    ): AccountVideoSubscriptionsCache?

    suspend fun saveVideoSubscriptions(cache: AccountVideoSubscriptionsCache)

    suspend fun deleteVideoSubscriptions(userId: Int)

    suspend fun getNotifications(
        userId: Int,
        language: String,
        limit: Int,
        offset: Int,
    ): AccountNotificationsPageCache?

    suspend fun saveNotifications(
        cache: AccountNotificationsPageCache,
        prunePagesCachedBefore: Long? = null,
    )

    suspend fun deleteNotifications(userId: Int)

    suspend fun getUserFriends(
        userId: Int,
        language: String,
        limit: Int,
        offset: Int,
    ): AccountUserFriendsPageCache?

    suspend fun saveUserFriends(cache: AccountUserFriendsPageCache)

    suspend fun deleteUserFriends(userId: Int)

    suspend fun getUserReviews(
        userId: Int,
        language: String,
        limit: Int,
        offset: Int,
    ): AccountUserReviewsPageCache?

    suspend fun saveUserReviews(cache: AccountUserReviewsPageCache)

    suspend fun getUserPosts(
        userId: Int,
        language: String,
        limit: Int,
        offset: Int,
    ): AccountUserPostsPageCache?

    suspend fun saveUserPosts(cache: AccountUserPostsPageCache)

    suspend fun getNotificationCounts(userId: Int): AccountNotificationCountsCache?

    suspend fun saveNotificationCounts(cache: AccountNotificationCountsCache)

    suspend fun deleteNotificationCounts(userId: Int)

    suspend fun getNotificationAnime(slug: String): AccountNotificationAnimeEntry?

    suspend fun saveNotificationAnime(entry: AccountNotificationAnimeEntry)

    suspend fun getUserStats(userId: Int, language: String): AccountUserStatsCache?

    suspend fun saveUserStats(cache: AccountUserStatsCache)

    suspend fun getUserProfileSummary(
        userId: Int,
        language: String,
    ): AccountUserProfileSummaryCache?

    suspend fun saveUserProfileSummary(cache: AccountUserProfileSummaryCache)

    suspend fun deleteUserProfileSummary(userId: Int)

    suspend fun clearUserScoped(userId: Int)
}
