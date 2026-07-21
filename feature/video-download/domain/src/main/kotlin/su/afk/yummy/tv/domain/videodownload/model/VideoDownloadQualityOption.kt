package su.afk.yummy.tv.domain.videodownload.model

data class VideoDownloadQualityOption(
    val label: String,
    val url: String,
    val headers: Map<String, String> = emptyMap(),
)
