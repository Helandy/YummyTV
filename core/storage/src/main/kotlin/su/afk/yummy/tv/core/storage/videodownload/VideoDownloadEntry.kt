package su.afk.yummy.tv.core.storage.videodownload

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "video_downloads",
    indices = [
        Index(
            value = ["animeId", "videoId", "iframeUrl", "qualityLabel"],
            name = "index_video_downloads_duplicate_key",
            unique = true,
        ),
        Index(value = ["status"], name = "index_video_downloads_status"),
        Index(value = ["updatedAt"], name = "index_video_downloads_updatedAt"),
    ],
)
data class VideoDownloadEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val animeId: Int,
    val animeTitle: String,
    val posterUrl: String,
    val episode: String,
    val videoId: Int,
    val playerName: String,
    val playerId: Int?,
    val dubbing: String,
    val iframeUrl: String,
    val screenshotUrl: String,
    val qualityLabel: String,
    val streamUrl: String,
    val headersJson: String,
    val cacheKey: String,
    val status: String,
    val progress: Float,
    val bytesDownloaded: Long,
    val totalBytes: Long?,
    val errorMessage: String?,
    val createdAt: Long,
    val updatedAt: Long,
)
