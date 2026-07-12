package su.afk.yummy.tv.core.storage.top

import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "anime_top_pages",
    primaryKeys = ["type", "language", "limit", "offset"],
    indices = [
        Index(value = ["cachedAt"], name = "index_anime_top_pages_cachedAt"),
    ],
)
data class AnimeTopPageEntry(
    val type: String,
    val language: String,
    val limit: Int,
    val offset: Int,
    val responseSize: Int,
    val cachedAt: Long,
)

@Entity(
    tableName = "anime_top_items",
    primaryKeys = ["type", "language", "limit", "offset", "position"],
    indices = [
        Index(
            value = ["type", "language", "limit", "offset"],
            name = "index_anime_top_items_page",
        ),
    ],
)
data class AnimeTopItemEntry(
    val type: String,
    val language: String,
    val limit: Int,
    val offset: Int,
    val position: Int,
    val animeId: Int,
    val title: String,
    val posterUrl: String? = null,
    val rating: Double? = null,
    val year: Int? = null,
)
