package su.afk.yummy.tv.domain.videodownload.model

data class VideoDownloadItem(
    val id: Long,
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
    val headers: Map<String, String>,
    val cacheKey: String,
    val status: VideoDownloadStatus,
    val progress: Float,
    val bytesDownloaded: Long,
    val totalBytes: Long?,
    val errorMessage: String?,
    val createdAt: Long,
    val updatedAt: Long,
)

enum class VideoDownloadStatus {
    Idle,
    Resolving,
    Queued,
    Downloading,
    Paused,
    Downloaded,
    Failed,
    Deleting,
    Deleted,
}

data class VideoDownloadQualityOption(
    val label: String,
    val url: String,
    val headers: Map<String, String> = emptyMap(),
)

data class VideoDownloadRequest(
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
    val quality: VideoDownloadQualityOption,
    val headers: Map<String, String>,
)

data class VideoDownloadRestartStream(
    val qualityLabel: String,
    val url: String,
    val headers: Map<String, String>,
)

sealed interface VideoDownloadStreamRefreshResult {
    data class Success(val stream: VideoDownloadRestartStream) : VideoDownloadStreamRefreshResult
    data class Failure(val message: String) : VideoDownloadStreamRefreshResult
}
