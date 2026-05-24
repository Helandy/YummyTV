package su.afk.yummy.tv.core.storage.watchprogress

import androidx.room.Entity

@Entity(tableName = "watch_progress", primaryKeys = ["animeId", "episode"])
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
