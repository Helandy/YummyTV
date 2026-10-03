package su.afk.yummy.tv.core.storage.account

import androidx.room.Entity
import androidx.room.Index

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
