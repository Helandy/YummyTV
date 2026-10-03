package su.afk.yummy.tv.core.storage.account

import androidx.room.Entity
import androidx.room.Index

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
