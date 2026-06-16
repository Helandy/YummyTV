package su.afk.yummy.tv.core.storage.home

import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "home_feed_caches",
    primaryKeys = ["language", "watchSignature"],
    indices = [
        Index(value = ["cachedAt"], name = "index_home_feed_caches_cachedAt"),
    ],
)
data class HomeFeedCacheEntry(
    val language: String,
    val watchSignature: String = HOME_FEED_GENERIC_WATCH_SIGNATURE,
    val cachedAt: Long,
)

@Entity(
    tableName = "home_feed_items",
    primaryKeys = ["language", "watchSignature", "container", "position"],
    indices = [
        Index(
            value = ["language", "watchSignature", "container"],
            name = "index_home_feed_items_language_signature_container",
        ),
    ],
)
data class HomeFeedItemEntry(
    val language: String,
    val watchSignature: String = HOME_FEED_GENERIC_WATCH_SIGNATURE,
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
    val episode: String = "",
    val episodeUrl: String = "",
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val updatedAt: Long = 0L,
    val playerName: String = "",
    val dubbing: String = "",
    val screenshotUrl: String = "",
)
