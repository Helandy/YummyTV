package su.afk.yummy.tv.core.storage.watchprogress

import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "remote_continue_watching",
    primaryKeys = ["accountKey", "language", "animeId", "targetKey"],
    indices = [
        Index(
            value = ["accountKey", "language", "updatedAt"],
            name = "index_remote_continue_watching_account_language_updatedAt",
        ),
        Index(value = ["animeId"], name = "index_remote_continue_watching_animeId"),
    ],
)
data class RemoteContinueWatchingEntry(
    val accountKey: String,
    val language: String,
    val animeId: Int,
    val targetKey: String,
    val episode: String,
    val videoId: Int = 0,
    val episodeUrl: String,
    val positionMs: Long,
    val durationMs: Long,
    val updatedAt: Long,
    val animeTitle: String = "",
    val posterUrl: String = "",
    val playerName: String = "",
    val dubbing: String = "",
    val screenshotUrl: String = "",
    val cachedAt: Long = System.currentTimeMillis(),
)
