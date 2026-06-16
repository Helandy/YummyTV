package su.afk.yummy.tv.core.storage.account

import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "account_profiles",
    primaryKeys = ["profileKey"],
    indices = [
        Index(value = ["userId"], name = "index_account_profiles_userId"),
        Index(value = ["cachedAt"], name = "index_account_profiles_cachedAt"),
    ],
)
data class AccountProfileEntry(
    val profileKey: String,
    val userId: Int,
    val nickname: String,
    val avatarUrl: String? = null,
    val cachedAt: Long,
)

@Entity(
    tableName = "account_user_list_pages",
    primaryKeys = ["userId", "listId", "language"],
    indices = [
        Index(value = ["cachedAt"], name = "index_account_user_list_pages_cachedAt"),
    ],
)
data class AccountUserListPageEntry(
    val userId: Int,
    val listId: Int,
    val language: String,
    val cachedAt: Long,
)

@Entity(
    tableName = "account_user_list_items",
    primaryKeys = ["userId", "listId", "language", "position"],
    indices = [
        Index(
            value = ["userId", "listId", "language"],
            name = "index_account_user_list_items_page",
        ),
    ],
)
data class AccountUserListItemEntry(
    val userId: Int,
    val listId: Int,
    val language: String,
    val position: Int,
    val animeId: Int,
    val title: String,
    val posterUrl: String? = null,
    val posterSmallUrl: String? = null,
    val posterMediumUrl: String? = null,
    val posterBigUrl: String? = null,
    val posterFullsizeUrl: String? = null,
    val posterMegaUrl: String? = null,
    val rating: Double? = null,
    val year: Int? = null,
    val userListId: Int? = null,
    val isFavorite: Boolean,
    val updatedAtSeconds: Long? = null,
)

@Entity(
    tableName = "account_anime_list_states",
    primaryKeys = ["userId", "animeId"],
    indices = [
        Index(value = ["cachedAt"], name = "index_account_anime_list_states_cachedAt"),
    ],
)
data class AccountAnimeListStateEntry(
    val userId: Int,
    val animeId: Int,
    val listId: Int? = null,
    val isFavorite: Boolean,
    val cachedAt: Long,
)

@Entity(
    tableName = "account_rating_bucket_caches",
    primaryKeys = ["animeId"],
    indices = [
        Index(value = ["cachedAt"], name = "index_account_rating_bucket_caches_cachedAt"),
    ],
)
data class AccountRatingBucketCacheEntry(
    val animeId: Int,
    val cachedAt: Long,
)

@Entity(
    tableName = "account_rating_buckets",
    primaryKeys = ["animeId", "position"],
    indices = [
        Index(value = ["animeId"], name = "index_account_rating_buckets_animeId"),
    ],
)
data class AccountRatingBucketEntry(
    val animeId: Int,
    val position: Int,
    val rating: Int,
    val count: Int,
)

@Entity(
    tableName = "account_user_ratings",
    primaryKeys = ["userId", "animeId"],
    indices = [
        Index(value = ["cachedAt"], name = "index_account_user_ratings_cachedAt"),
    ],
)
data class AccountUserRatingEntry(
    val userId: Int,
    val animeId: Int,
    val rating: Int? = null,
    val cachedAt: Long,
)

@Entity(
    tableName = "account_list_stats_caches",
    primaryKeys = ["animeId"],
    indices = [
        Index(value = ["cachedAt"], name = "index_account_list_stats_caches_cachedAt"),
    ],
)
data class AccountListStatsCacheEntry(
    val animeId: Int,
    val cachedAt: Long,
)

@Entity(
    tableName = "account_list_stats",
    primaryKeys = ["animeId", "listId"],
    indices = [
        Index(value = ["animeId"], name = "index_account_list_stats_animeId"),
    ],
)
data class AccountListStatEntry(
    val animeId: Int,
    val listId: Int,
    val count: Int,
)

@Entity(
    tableName = "account_collection_pages",
    primaryKeys = ["pageKey"],
    indices = [
        Index(value = ["language"], name = "index_account_collection_pages_language"),
        Index(value = ["cachedAt"], name = "index_account_collection_pages_cachedAt"),
    ],
)
data class AccountCollectionPageEntry(
    val pageKey: String,
    val language: String,
    val cachedAt: Long,
)

@Entity(
    tableName = "account_collection_items",
    primaryKeys = ["pageKey", "position"],
    indices = [
        Index(value = ["pageKey"], name = "index_account_collection_items_pageKey"),
    ],
)
data class AccountCollectionItemEntry(
    val pageKey: String,
    val position: Int,
    val collectionId: Int,
    val title: String,
    val description: String,
    val posterUrl: String? = null,
    val posterSmallUrl: String? = null,
    val posterMediumUrl: String? = null,
    val posterBigUrl: String? = null,
    val posterFullsizeUrl: String? = null,
    val posterMegaUrl: String? = null,
    val views: Int? = null,
)

@Entity(
    tableName = "account_video_subscription_caches",
    primaryKeys = ["userId", "language"],
    indices = [
        Index(value = ["cachedAt"], name = "index_account_video_subscription_caches_cachedAt"),
    ],
)
data class AccountVideoSubscriptionCacheEntry(
    val userId: Int,
    val language: String,
    val cachedAt: Long,
)

@Entity(
    tableName = "account_video_subscriptions",
    primaryKeys = ["userId", "language", "position"],
    indices = [
        Index(
            value = ["userId", "language"],
            name = "index_account_video_subscriptions_userId_language",
        ),
    ],
)
data class AccountVideoSubscriptionEntry(
    val userId: Int,
    val language: String,
    val position: Int,
    val animeId: Int,
    val animeUrl: String,
    val playerId: Int? = null,
    val player: String,
    val dubbing: String,
    val posterUrl: String? = null,
    val title: String,
)

@Entity(
    tableName = "account_notification_pages",
    primaryKeys = ["userId", "language", "limit", "offset"],
    indices = [
        Index(value = ["cachedAt"], name = "index_account_notification_pages_cachedAt"),
    ],
)
data class AccountNotificationPageEntry(
    val userId: Int,
    val language: String,
    val limit: Int,
    val offset: Int,
    val cachedAt: Long,
)

@Entity(
    tableName = "account_notifications",
    primaryKeys = ["userId", "language", "limit", "offset", "position"],
    indices = [
        Index(
            value = ["userId", "language", "limit", "offset"],
            name = "index_account_notifications_page",
        ),
    ],
)
data class AccountNotificationEntry(
    val userId: Int,
    val language: String,
    val limit: Int,
    val offset: Int,
    val position: Int,
    val notificationId: Int,
    val dateSeconds: Long,
    val title: String,
    val text: String,
    val clickUri: String,
    val type: String,
    val subType: String,
    val viewed: Boolean,
    val objectId: Int? = null,
    val animeSlug: String? = null,
    val isNewEpisode: Boolean,
)

@Entity(
    tableName = "account_notification_count_caches",
    primaryKeys = ["userId"],
    indices = [
        Index(value = ["cachedAt"], name = "index_account_notification_count_caches_cachedAt"),
    ],
)
data class AccountNotificationCountCacheEntry(
    val userId: Int,
    val cachedAt: Long,
)

@Entity(
    tableName = "account_notification_counts",
    primaryKeys = ["userId", "position"],
    indices = [
        Index(value = ["userId"], name = "index_account_notification_counts_userId"),
    ],
)
data class AccountNotificationCountEntry(
    val userId: Int,
    val position: Int,
    val type: String,
    val count: Int,
)

@Entity(
    tableName = "account_notification_anime",
    primaryKeys = ["slug"],
    indices = [
        Index(value = ["cachedAt"], name = "index_account_notification_anime_cachedAt"),
    ],
)
data class AccountNotificationAnimeEntry(
    val slug: String,
    val animeId: Int? = null,
    val cachedAt: Long,
)

@Entity(
    tableName = "account_user_stats_caches",
    primaryKeys = ["userId", "language"],
    indices = [
        Index(value = ["cachedAt"], name = "index_account_user_stats_caches_cachedAt"),
    ],
)
data class AccountUserStatsCacheEntry(
    val userId: Int,
    val language: String,
    val cachedAt: Long,
)

@Entity(
    tableName = "account_user_genre_stats",
    primaryKeys = ["userId", "language", "position"],
    indices = [
        Index(
            value = ["userId", "language"],
            name = "index_account_user_genre_stats_userId_language"
        ),
    ],
)
data class AccountUserGenreStatEntry(
    val userId: Int,
    val language: String,
    val position: Int,
    val genreId: Int,
    val title: String,
    val count: Int,
)

@Entity(
    tableName = "account_user_rating_stats",
    primaryKeys = ["userId", "language", "position"],
    indices = [
        Index(
            value = ["userId", "language"],
            name = "index_account_user_rating_stats_userId_language"
        ),
    ],
)
data class AccountUserRatingStatEntry(
    val userId: Int,
    val language: String,
    val position: Int,
    val rating: Int,
    val count: Int,
)

@Entity(
    tableName = "account_user_list_watch_stats",
    primaryKeys = ["userId", "language", "position"],
    indices = [
        Index(
            value = ["userId", "language"],
            name = "index_account_user_list_watch_stats_userId_language"
        ),
    ],
)
data class AccountUserListWatchStatEntry(
    val userId: Int,
    val language: String,
    val position: Int,
    val listId: Int,
    val title: String,
    val href: String,
    val seconds: Long,
)

@Entity(
    tableName = "account_user_type_stats",
    primaryKeys = ["userId", "language", "position"],
    indices = [
        Index(
            value = ["userId", "language"],
            name = "index_account_user_type_stats_userId_language"
        ),
    ],
)
data class AccountUserTypeStatEntry(
    val userId: Int,
    val language: String,
    val position: Int,
    val typeId: Int,
    val title: String,
    val shortName: String,
    val count: Int,
)

@Entity(
    tableName = "account_user_profile_summary_caches",
    primaryKeys = ["userId", "language"],
    indices = [
        Index(value = ["cachedAt"], name = "index_account_user_profile_summary_caches_cachedAt"),
    ],
)
data class AccountUserProfileSummaryCacheEntry(
    val userId: Int,
    val language: String,
    val cachedAt: Long,
    val nickname: String,
    val avatarUrl: String? = null,
    val bannerUrl: String? = null,
    val registerDateSeconds: Long,
    val birthDateSeconds: Long,
    val sex: Int,
    val about: String,
    val daysOnline: Int,
    val watchingCount: Int,
    val plannedCount: Int,
    val completedCount: Int,
    val droppedCount: Int,
    val postponedCount: Int,
    val favoriteCount: Int,
    val friendsCount: Int,
    val reviewsCount: Int,
    val commentsCount: Int,
    val postsCount: Int,
    val collectionsCount: Int,
)

@Entity(
    tableName = "account_user_profile_watch_types",
    primaryKeys = ["userId", "language", "position"],
    indices = [
        Index(
            value = ["userId", "language"],
            name = "index_account_user_profile_watch_types_userId_language",
        ),
    ],
)
data class AccountUserProfileWatchTypeEntry(
    val userId: Int,
    val language: String,
    val position: Int,
    val typeId: Int,
    val alias: String,
    val title: String,
    val shortName: String,
    val spentSeconds: Long,
)

@Entity(
    tableName = "account_user_profile_watch_history",
    primaryKeys = ["userId", "language", "position"],
    indices = [
        Index(
            value = ["userId", "language"],
            name = "index_account_user_profile_watch_history_userId_language",
        ),
    ],
)
data class AccountUserProfileWatchHistoryEntry(
    val userId: Int,
    val language: String,
    val position: Int,
    val dateSeconds: Long,
    val durationSeconds: Long,
    val episodeCount: Int,
)
