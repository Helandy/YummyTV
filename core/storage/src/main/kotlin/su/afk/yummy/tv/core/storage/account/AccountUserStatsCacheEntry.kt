package su.afk.yummy.tv.core.storage.account

import androidx.room.Entity
import androidx.room.Index

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
