package su.afk.yummy.tv.core.storage.account

import androidx.room.withTransaction
import su.afk.yummy.tv.core.storage.db.AppDatabase

internal class AccountStorageStore(
    private val db: AppDatabase,
    private val animeRatingsDao: AccountAnimeRatingsDao,
    private val collectionsDao: AccountCollectionsDao,
    private val notificationsDao: AccountNotificationsDao,
    private val profileDao: AccountProfileDao,
    private val userListsDao: AccountUserListsDao,
    private val userProfileDao: AccountUserProfileDao,
    private val videoSubscriptionsDao: AccountVideoSubscriptionsDao,
) : AccountStorage {

    override suspend fun getProfile(profileKey: String): AccountProfileEntry? =
        profileDao.getProfile(profileKey)

    override suspend fun saveProfile(entry: AccountProfileEntry) {
        profileDao.insertProfile(entry)
    }

    override suspend fun deleteProfile(profileKey: String) {
        profileDao.deleteProfile(profileKey)
    }

    override suspend fun getUserList(
        userId: Int,
        listId: Int,
        language: String,
    ): AccountUserListCache? =
        userListsDao.getUserList(userId, listId, language)

    override suspend fun saveUserList(cache: AccountUserListCache) {
        userListsDao.replaceUserList(cache)
    }

    override suspend fun saveUserLists(caches: List<AccountUserListCache>) {
        userListsDao.replaceUserLists(caches)
    }

    override suspend fun hasUserListCache(userId: Int): Boolean =
        userListsDao.hasUserListPages(userId)

    override suspend fun deleteUserLists(userId: Int) {
        userListsDao.deleteUserLists(userId)
    }

    override suspend fun getAnimeListState(userId: Int, animeId: Int): AccountAnimeListStateEntry? =
        userListsDao.getAnimeListState(userId, animeId)

    override suspend fun saveAnimeListState(entry: AccountAnimeListStateEntry) {
        userListsDao.insertAnimeListState(entry)
    }

    override suspend fun getRatingBuckets(animeId: Int): AccountRatingBucketsCache? =
        animeRatingsDao.getRatingBuckets(animeId)

    override suspend fun saveRatingBuckets(cache: AccountRatingBucketsCache) {
        animeRatingsDao.replaceRatingBuckets(cache)
    }

    override suspend fun deleteRatingBuckets(animeId: Int) {
        animeRatingsDao.deleteRatingBucketsCache(animeId)
    }

    override suspend fun getUserRating(userId: Int, animeId: Int): AccountUserRatingEntry? =
        animeRatingsDao.getUserRating(userId, animeId)

    override suspend fun saveUserRating(entry: AccountUserRatingEntry) {
        animeRatingsDao.insertUserRating(entry)
    }

    override suspend fun getListStats(animeId: Int): AccountListStatsCache? =
        animeRatingsDao.getListStats(animeId)

    override suspend fun saveListStats(cache: AccountListStatsCache) {
        animeRatingsDao.replaceListStats(cache)
    }

    override suspend fun invalidateListStats(animeId: Int) {
        animeRatingsDao.invalidateListStats(animeId)
    }

    override suspend fun getCollections(pageKey: String): AccountCollectionsPageCache? =
        collectionsDao.getCollections(pageKey)

    override suspend fun saveCollections(
        cache: AccountCollectionsPageCache,
        prunePagesCachedBefore: Long?,
    ) {
        collectionsDao.replaceCollections(cache, prunePagesCachedBefore)
    }

    override suspend fun invalidateCollections() {
        collectionsDao.invalidateCollections()
    }

    override suspend fun getVideoSubscriptions(
        userId: Int,
        language: String,
    ): AccountVideoSubscriptionsCache? =
        videoSubscriptionsDao.getVideoSubscriptions(userId, language)

    override suspend fun saveVideoSubscriptions(cache: AccountVideoSubscriptionsCache) {
        videoSubscriptionsDao.replaceVideoSubscriptions(cache)
    }

    override suspend fun deleteVideoSubscriptions(userId: Int) {
        videoSubscriptionsDao.deleteVideoSubscriptionsForUser(userId)
    }

    override suspend fun getNotifications(
        userId: Int,
        language: String,
        limit: Int,
        offset: Int,
    ): AccountNotificationsPageCache? =
        notificationsDao.getNotifications(userId, language, limit, offset)

    override suspend fun saveNotifications(
        cache: AccountNotificationsPageCache,
        prunePagesCachedBefore: Long?,
    ) {
        notificationsDao.replaceNotifications(cache, prunePagesCachedBefore)
    }

    override suspend fun deleteNotifications(userId: Int) {
        notificationsDao.deleteNotificationsForUser(userId)
    }

    override suspend fun getUserFriends(
        userId: Int,
        language: String,
        limit: Int,
        offset: Int,
    ): AccountUserFriendsPageCache? =
        userProfileDao.getUserFriendsPage(userId, language, limit, offset)

    override suspend fun saveUserFriends(cache: AccountUserFriendsPageCache) {
        userProfileDao.replaceUserFriendsPage(cache)
    }

    override suspend fun deleteUserFriends(userId: Int) {
        userProfileDao.deleteUserFriendsContentForUser(userId)
    }

    override suspend fun getUserReviews(
        userId: Int,
        language: String,
        limit: Int,
        offset: Int,
    ): AccountUserReviewsPageCache? =
        userProfileDao.getUserReviewsPage(userId, language, limit, offset)

    override suspend fun saveUserReviews(cache: AccountUserReviewsPageCache) {
        userProfileDao.replaceUserReviewsPage(cache)
    }

    override suspend fun getUserPosts(
        userId: Int,
        language: String,
        limit: Int,
        offset: Int,
    ): AccountUserPostsPageCache? =
        userProfileDao.getUserPostsPage(userId, language, limit, offset)

    override suspend fun saveUserPosts(cache: AccountUserPostsPageCache) {
        userProfileDao.replaceUserPostsPage(cache)
    }

    override suspend fun getNotificationCounts(userId: Int): AccountNotificationCountsCache? =
        notificationsDao.getNotificationCounts(userId)

    override suspend fun saveNotificationCounts(cache: AccountNotificationCountsCache) {
        notificationsDao.replaceNotificationCounts(cache)
    }

    override suspend fun deleteNotificationCounts(userId: Int) {
        notificationsDao.deleteNotificationCountCache(userId)
        notificationsDao.deleteNotificationCounts(userId)
    }

    override suspend fun getNotificationAnime(slug: String): AccountNotificationAnimeEntry? =
        notificationsDao.getNotificationAnime(slug)

    override suspend fun saveNotificationAnime(entry: AccountNotificationAnimeEntry) {
        notificationsDao.insertNotificationAnime(entry)
    }

    override suspend fun getUserStats(userId: Int, language: String): AccountUserStatsCache? =
        userProfileDao.getUserStats(userId, language)

    override suspend fun saveUserStats(cache: AccountUserStatsCache) {
        userProfileDao.replaceUserStats(cache)
    }

    override suspend fun getUserProfileSummary(
        userId: Int,
        language: String,
    ): AccountUserProfileSummaryCache? =
        userProfileDao.getUserProfileSummary(userId, language)

    override suspend fun saveUserProfileSummary(cache: AccountUserProfileSummaryCache) {
        userProfileDao.replaceUserProfileSummary(cache)
    }

    override suspend fun deleteUserProfileSummary(userId: Int) {
        userProfileDao.deleteUserProfileSummaryForUser(userId)
    }

    override suspend fun clearUserScoped(userId: Int) {
        db.withTransaction {
            profileDao.deleteProfile(ACCOUNT_PROFILE_KEY_CURRENT)
            profileDao.deleteProfilesByUser(userId)
            userListsDao.deleteUserLists(userId)
            userListsDao.deleteAnimeListStates(userId)
            animeRatingsDao.deleteUserRatings(userId)
            videoSubscriptionsDao.deleteVideoSubscriptionsForUser(userId)
            notificationsDao.deleteNotificationsForUser(userId)
            notificationsDao.deleteNotificationCountCache(userId)
            notificationsDao.deleteNotificationCounts(userId)
            userProfileDao.deleteUserProfileContentForUser(userId)
            userProfileDao.deleteUserStatsForUser(userId)
        }
    }
}
