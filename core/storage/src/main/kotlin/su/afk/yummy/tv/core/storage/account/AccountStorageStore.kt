package su.afk.yummy.tv.core.storage.account

class AccountStorageStore(private val dao: AccountStorageDao) {

    suspend fun getProfile(profileKey: String): AccountProfileEntry? =
        dao.getProfile(profileKey)

    suspend fun saveProfile(entry: AccountProfileEntry) {
        dao.insertProfile(entry)
    }

    suspend fun deleteProfile(profileKey: String) {
        dao.deleteProfile(profileKey)
    }

    suspend fun getUserList(
        userId: Int,
        listId: Int,
        language: String,
    ): AccountUserListCache? =
        dao.getUserList(userId, listId, language)

    suspend fun saveUserList(cache: AccountUserListCache) {
        dao.replaceUserList(cache)
    }

    suspend fun deleteUserLists(userId: Int) {
        dao.deleteUserLists(userId)
    }

    suspend fun getAnimeListState(userId: Int, animeId: Int): AccountAnimeListStateEntry? =
        dao.getAnimeListState(userId, animeId)

    suspend fun saveAnimeListState(entry: AccountAnimeListStateEntry) {
        dao.insertAnimeListState(entry)
    }

    suspend fun getRatingBuckets(animeId: Int): AccountRatingBucketsCache? =
        dao.getRatingBuckets(animeId)

    suspend fun saveRatingBuckets(cache: AccountRatingBucketsCache) {
        dao.replaceRatingBuckets(cache)
    }

    suspend fun deleteRatingBuckets(animeId: Int) {
        dao.deleteRatingBucketsCache(animeId)
    }

    suspend fun getUserRating(userId: Int, animeId: Int): AccountUserRatingEntry? =
        dao.getUserRating(userId, animeId)

    suspend fun saveUserRating(entry: AccountUserRatingEntry) {
        dao.insertUserRating(entry)
    }

    suspend fun getListStats(animeId: Int): AccountListStatsCache? =
        dao.getListStats(animeId)

    suspend fun saveListStats(cache: AccountListStatsCache) {
        dao.replaceListStats(cache)
    }

    suspend fun getCollections(pageKey: String): AccountCollectionsPageCache? =
        dao.getCollections(pageKey)

    suspend fun saveCollections(
        cache: AccountCollectionsPageCache,
        prunePagesCachedBefore: Long? = null,
    ) {
        dao.replaceCollections(cache, prunePagesCachedBefore)
    }

    suspend fun getVideoSubscriptions(
        userId: Int,
        language: String,
    ): AccountVideoSubscriptionsCache? =
        dao.getVideoSubscriptions(userId, language)

    suspend fun saveVideoSubscriptions(cache: AccountVideoSubscriptionsCache) {
        dao.replaceVideoSubscriptions(cache)
    }

    suspend fun deleteVideoSubscriptions(userId: Int) {
        dao.deleteVideoSubscriptionsForUser(userId)
    }

    suspend fun getNotifications(
        userId: Int,
        language: String,
        limit: Int,
        offset: Int,
    ): AccountNotificationsPageCache? =
        dao.getNotifications(userId, language, limit, offset)

    suspend fun saveNotifications(
        cache: AccountNotificationsPageCache,
        prunePagesCachedBefore: Long? = null,
    ) {
        dao.replaceNotifications(cache, prunePagesCachedBefore)
    }

    suspend fun deleteNotifications(userId: Int) {
        dao.deleteNotificationsForUser(userId)
    }

    suspend fun getNotificationCounts(userId: Int): AccountNotificationCountsCache? =
        dao.getNotificationCounts(userId)

    suspend fun saveNotificationCounts(cache: AccountNotificationCountsCache) {
        dao.replaceNotificationCounts(cache)
    }

    suspend fun deleteNotificationCounts(userId: Int) {
        dao.deleteNotificationCountCache(userId)
        dao.deleteNotificationCounts(userId)
    }

    suspend fun getNotificationAnime(slug: String): AccountNotificationAnimeEntry? =
        dao.getNotificationAnime(slug)

    suspend fun saveNotificationAnime(entry: AccountNotificationAnimeEntry) {
        dao.insertNotificationAnime(entry)
    }

    suspend fun getUserStats(userId: Int, language: String): AccountUserStatsCache? =
        dao.getUserStats(userId, language)

    suspend fun saveUserStats(cache: AccountUserStatsCache) {
        dao.replaceUserStats(cache)
    }

    suspend fun getUserProfileSummary(
        userId: Int,
        language: String,
    ): AccountUserProfileSummaryCache? =
        dao.getUserProfileSummary(userId, language)

    suspend fun saveUserProfileSummary(cache: AccountUserProfileSummaryCache) {
        dao.replaceUserProfileSummary(cache)
    }

    suspend fun clearUserScoped(userId: Int) {
        dao.clearUserScoped(userId)
    }
}
