package su.afk.yummy.tv.core.storage.watchprogress

import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "watch_progress",
    primaryKeys = ["animeId", "episode"],
    indices = [
        Index(value = ["updatedAt"], name = "index_watch_progress_updatedAt"),
    ],
)
data class WatchProgressEntry(
    val animeId: Int,
    val episode: String,
    val videoId: Int = 0,
    val episodeUrl: String,
    val positionMs: Long,
    val durationMs: Long,
    val updatedAt: Long = System.currentTimeMillis(),
    val animeTitle: String = "",
    val posterUrl: String = "",
    val playerName: String = "",
    val dubbing: String = "",
    val screenshotUrl: String = "",
)
