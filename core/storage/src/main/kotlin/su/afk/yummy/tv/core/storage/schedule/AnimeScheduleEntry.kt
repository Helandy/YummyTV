package su.afk.yummy.tv.core.storage.schedule

import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "anime_schedule_caches",
    primaryKeys = ["language"],
    indices = [
        Index(value = ["cachedAt"], name = "index_anime_schedule_caches_cachedAt"),
    ],
)
data class AnimeScheduleCacheEntry(
    val language: String,
    val cachedAt: Long,
)

@Entity(
    tableName = "anime_schedule_items",
    primaryKeys = ["language", "position"],
    indices = [
        Index(value = ["language"], name = "index_anime_schedule_items_language"),
    ],
)
data class AnimeScheduleItemEntry(
    val language: String,
    val position: Int,
    val animeId: Int,
    val title: String,
    val posterUrl: String? = null,
    val nextDateEpochSeconds: Long? = null,
    val previousDateEpochSeconds: Long? = null,
    val airedEpisodes: Int? = null,
    val totalEpisodes: Int? = null,
)
