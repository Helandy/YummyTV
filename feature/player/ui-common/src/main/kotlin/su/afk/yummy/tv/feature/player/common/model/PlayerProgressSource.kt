package su.afk.yummy.tv.feature.player.common.model

data class PlayerProgressSource(
    val episodeUrl: String,
    val episode: String,
    val videoId: Int,
    val playerName: String,
    val dubbing: String,
    val screenshotUrl: String,
)
