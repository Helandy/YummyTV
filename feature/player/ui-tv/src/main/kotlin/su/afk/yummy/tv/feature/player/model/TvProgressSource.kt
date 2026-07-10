package su.afk.yummy.tv.feature.player.model

internal data class TvProgressSource(
    val episodeUrl: String,
    val episode: String,
    val videoId: Int,
    val playerName: String,
    val dubbing: String,
    val screenshotUrl: String,
)
