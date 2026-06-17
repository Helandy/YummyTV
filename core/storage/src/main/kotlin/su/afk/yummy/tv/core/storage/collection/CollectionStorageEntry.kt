package su.afk.yummy.tv.core.storage.collection

import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "collection_details",
    primaryKeys = ["collectionId", "language"],
    indices = [
        Index(value = ["cachedAt"], name = "index_collection_details_cachedAt"),
    ],
)
data class CollectionDetailEntry(
    val collectionId: Int,
    val language: String,
    val title: String,
    val description: String,
    val views: Int,
    val posterUrl: String? = null,
    val cachedAt: Long,
)

@Entity(
    tableName = "collection_anime_items",
    primaryKeys = ["collectionId", "language", "position"],
    indices = [
        Index(
            value = ["collectionId", "language"],
            name = "index_collection_anime_items_collectionId_language",
        ),
    ],
)
data class CollectionAnimeItemEntry(
    val collectionId: Int,
    val language: String,
    val position: Int,
    val animeId: Int,
    val title: String,
    val posterUrl: String? = null,
    val rating: Double? = null,
)

@Entity(
    tableName = "collection_catalog_pages",
    primaryKeys = ["pageKey"],
    indices = [
        Index(value = ["language"], name = "index_collection_catalog_pages_language"),
        Index(value = ["cachedAt"], name = "index_collection_catalog_pages_cachedAt"),
    ],
)
data class CollectionCatalogPageEntry(
    val pageKey: String,
    val language: String,
    val pageLimit: Int,
    val pageOffset: Int,
    val cachedAt: Long,
)

@Entity(
    tableName = "collection_catalog_items",
    primaryKeys = ["pageKey", "position"],
    indices = [
        Index(value = ["pageKey"], name = "index_collection_catalog_items_pageKey"),
    ],
)
data class CollectionCatalogItemEntry(
    val pageKey: String,
    val position: Int,
    val collectionId: Int,
    val title: String,
    val description: String,
    val posterUrl: String? = null,
)
