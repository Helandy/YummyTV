package su.afk.yummy.tv.core.storage.account

import androidx.room.Entity
import androidx.room.Index

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
