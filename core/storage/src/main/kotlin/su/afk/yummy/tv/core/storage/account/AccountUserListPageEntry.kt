package su.afk.yummy.tv.core.storage.account

import androidx.room.Entity
import androidx.room.Index

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
