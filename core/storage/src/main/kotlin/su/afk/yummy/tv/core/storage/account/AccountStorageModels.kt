package su.afk.yummy.tv.core.storage.account

const val ACCOUNT_PROFILE_KEY_CURRENT = "current"

fun accountProfileUserKey(userId: Int): String = "user:$userId"

data class AccountUserListCache(
    val entry: AccountUserListPageEntry,
    val items: List<AccountUserListItemEntry>,
) {
    val cachedAt: Long get() = entry.cachedAt
}

data class AccountRatingBucketsCache(
    val entry: AccountRatingBucketCacheEntry,
    val buckets: List<AccountRatingBucketEntry>,
) {
    val cachedAt: Long get() = entry.cachedAt
}

data class AccountListStatsCache(
    val entry: AccountListStatsCacheEntry,
    val stats: List<AccountListStatEntry>,
) {
    val cachedAt: Long get() = entry.cachedAt
}

data class AccountCollectionsPageCache(
    val entry: AccountCollectionPageEntry,
    val items: List<AccountCollectionItemEntry>,
) {
    val cachedAt: Long get() = entry.cachedAt
}

data class AccountVideoSubscriptionsCache(
    val entry: AccountVideoSubscriptionCacheEntry,
    val items: List<AccountVideoSubscriptionEntry>,
) {
    val cachedAt: Long get() = entry.cachedAt
}

data class AccountNotificationsPageCache(
    val entry: AccountNotificationPageEntry,
    val items: List<AccountNotificationEntry>,
) {
    val cachedAt: Long get() = entry.cachedAt
}

data class AccountNotificationCountsCache(
    val entry: AccountNotificationCountCacheEntry,
    val items: List<AccountNotificationCountEntry>,
) {
    val cachedAt: Long get() = entry.cachedAt
}

data class AccountUserStatsCache(
    val entry: AccountUserStatsCacheEntry,
    val genres: List<AccountUserGenreStatEntry>,
    val ratings: List<AccountUserRatingStatEntry>,
    val lists: List<AccountUserListWatchStatEntry>,
    val types: List<AccountUserTypeStatEntry>,
) {
    val cachedAt: Long get() = entry.cachedAt
}

fun AccountProfileEntry.isFresh(ttlMs: Long): Boolean =
    System.currentTimeMillis() - cachedAt < ttlMs

fun AccountUserListCache.isFresh(ttlMs: Long): Boolean =
    System.currentTimeMillis() - cachedAt < ttlMs

fun AccountAnimeListStateEntry.isFresh(ttlMs: Long): Boolean =
    System.currentTimeMillis() - cachedAt < ttlMs

fun AccountRatingBucketsCache.isFresh(ttlMs: Long): Boolean =
    System.currentTimeMillis() - cachedAt < ttlMs

fun AccountUserRatingEntry.isFresh(ttlMs: Long): Boolean =
    System.currentTimeMillis() - cachedAt < ttlMs

fun AccountListStatsCache.isFresh(ttlMs: Long): Boolean =
    System.currentTimeMillis() - cachedAt < ttlMs

fun AccountCollectionsPageCache.isFresh(ttlMs: Long): Boolean =
    System.currentTimeMillis() - cachedAt < ttlMs

fun AccountVideoSubscriptionsCache.isFresh(ttlMs: Long): Boolean =
    System.currentTimeMillis() - cachedAt < ttlMs

fun AccountNotificationsPageCache.isFresh(ttlMs: Long): Boolean =
    System.currentTimeMillis() - cachedAt < ttlMs

fun AccountNotificationCountsCache.isFresh(ttlMs: Long): Boolean =
    System.currentTimeMillis() - cachedAt < ttlMs

fun AccountNotificationAnimeEntry.isFresh(ttlMs: Long): Boolean =
    System.currentTimeMillis() - cachedAt < ttlMs

fun AccountUserStatsCache.isFresh(ttlMs: Long): Boolean =
    System.currentTimeMillis() - cachedAt < ttlMs
