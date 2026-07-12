package su.afk.yummy.tv.core.storage.search

import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "search_pages",
    primaryKeys = ["pageKey"],
    indices = [
        Index(value = ["language"], name = "index_search_pages_language"),
        Index(value = ["cachedAt"], name = "index_search_pages_cachedAt"),
    ],
)
data class SearchPageEntry(
    val pageKey: String,
    val language: String,
    val limit: Int,
    val offset: Int,
    val responseSize: Int,
    val cachedAt: Long,
)

@Entity(
    tableName = "search_items",
    primaryKeys = ["pageKey", "position"],
    indices = [
        Index(value = ["pageKey"], name = "index_search_items_pageKey"),
    ],
)
data class SearchItemEntry(
    val pageKey: String,
    val position: Int,
    val animeId: Int,
    val title: String,
    val posterUrl: String? = null,
    val rating: Double? = null,
    val year: Int? = null,
)

@Entity(
    tableName = "search_filter_options",
    primaryKeys = ["language"],
    indices = [
        Index(value = ["cachedAt"], name = "index_search_filter_options_cachedAt"),
    ],
)
data class SearchFilterOptionsEntry(
    val language: String,
    val cachedAt: Long,
)

@Entity(
    tableName = "search_genre_groups",
    primaryKeys = ["language", "position"],
    indices = [
        Index(value = ["language"], name = "index_search_genre_groups_language"),
    ],
)
data class SearchGenreGroupEntry(
    val language: String,
    val position: Int,
    val groupId: Int,
    val title: String,
)

@Entity(
    tableName = "search_genres",
    primaryKeys = ["language", "position"],
    indices = [
        Index(value = ["language"], name = "index_search_genres_language"),
    ],
)
data class SearchGenreEntry(
    val language: String,
    val position: Int,
    val genreId: String,
    val title: String,
    val groupId: Int,
)

@Entity(
    tableName = "search_types",
    primaryKeys = ["language", "position"],
    indices = [
        Index(value = ["language"], name = "index_search_types_language"),
    ],
)
data class SearchTypeEntry(
    val language: String,
    val position: Int,
    val typeId: String,
    val title: String,
)
