package su.afk.yummy.tv.core.storage.home

import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "home_feed_caches",
    primaryKeys = ["language"],
    indices = [
        Index(value = ["cachedAt"], name = "index_home_feed_caches_cachedAt"),
    ],
)
data class HomeFeedCacheEntry(
    val language: String,
    val cachedAt: Long,
)

@Entity(
    tableName = "home_feed_items",
    primaryKeys = ["language", "container", "position"],
    indices = [
        Index(value = ["language", "container"], name = "index_home_feed_items_language_container"),
    ],
)
data class HomeFeedItemEntry(
    val language: String,
    val container: String,
    val position: Int,
    val itemId: Int,
    val title: String,
    val description: String,
    val posterSmallUrl: String? = null,
    val posterMediumUrl: String? = null,
    val posterBigUrl: String? = null,
    val posterFullsizeUrl: String? = null,
    val posterMegaUrl: String? = null,
    val rating: Double? = null,
    val actionType: String,
    val actionId: Int,
)
