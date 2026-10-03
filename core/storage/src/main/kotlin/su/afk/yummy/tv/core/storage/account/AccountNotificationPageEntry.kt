package su.afk.yummy.tv.core.storage.account

import androidx.room.Entity
import androidx.room.Index

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
