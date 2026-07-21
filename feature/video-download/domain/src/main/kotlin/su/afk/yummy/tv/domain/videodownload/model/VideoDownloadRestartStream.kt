package su.afk.yummy.tv.domain.videodownload.model

data class VideoDownloadRestartStream(
    val videoId: Int,
    val playerName: String,
    val playerId: Int?,
    val dubbing: String,
    val iframeUrl: String,
    val qualityLabel: String,
    val url: String,
    val headers: Map<String, String>,
)
