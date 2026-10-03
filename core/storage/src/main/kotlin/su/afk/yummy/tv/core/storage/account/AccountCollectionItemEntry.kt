package su.afk.yummy.tv.core.storage.account

import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "account_collection_items",
    primaryKeys = ["pageKey", "position"],
    indices = [
        Index(value = ["pageKey"], name = "index_account_collection_items_pageKey"),
    ],
)
data class AccountCollectionItemEntry(
    val pageKey: String,
    val position: Int,
    val collectionId: Int,
    val title: String,
    val description: String,
    val posterUrl: String? = null,
    val posterSmallUrl: String? = null,
    val posterMediumUrl: String? = null,
    val posterBigUrl: String? = null,
    val posterFullsizeUrl: String? = null,
    val posterMegaUrl: String? = null,
    val views: Int? = null,
)
