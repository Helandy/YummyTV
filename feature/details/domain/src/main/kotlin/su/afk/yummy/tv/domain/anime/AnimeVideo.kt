package su.afk.yummy.tv.domain.anime

data class AnimeVideo(
    val id: Int,
    val episode: String,
    val dubbing: String,
    val player: String,
    val iframeUrl: String,
    val durationSeconds: Int?,
    val views: Int? = null,
)
