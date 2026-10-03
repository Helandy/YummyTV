package su.afk.yummy.tv.core.storage.account

import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "account_collection_pages",
    primaryKeys = ["pageKey"],
    indices = [
        Index(value = ["language"], name = "index_account_collection_pages_language"),
        Index(value = ["cachedAt"], name = "index_account_collection_pages_cachedAt"),
    ],
)
data class AccountCollectionPageEntry(
    val pageKey: String,
    val language: String,
    val cachedAt: Long,
)
