package su.afk.yummy.tv.core.storage.account

import androidx.room.Entity
import androidx.room.Index

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
