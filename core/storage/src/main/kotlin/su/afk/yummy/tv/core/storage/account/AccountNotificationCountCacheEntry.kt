package su.afk.yummy.tv.core.storage.account

import androidx.room.Entity
import androidx.room.Index

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
