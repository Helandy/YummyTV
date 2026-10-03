package su.afk.yummy.tv.core.storage.account

import androidx.room.Entity
import androidx.room.Index

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
