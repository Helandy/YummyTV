package su.afk.yummy.tv.core.storage.account

import androidx.room.Entity
import androidx.room.Index

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
