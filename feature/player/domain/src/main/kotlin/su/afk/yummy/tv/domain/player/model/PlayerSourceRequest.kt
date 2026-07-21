package su.afk.yummy.tv.domain.player.model

data class PlayerSourceRequest(
    val animeId: Int,
    val iframeUrl: String,
    val animeTitle: String,
    val episode: String,
    val playerName: String,
    val dubbing: String,
    val selectedVideoId: Int,
    val selectedPlayerId: Int? = null,
    val selectedScreenshotUrl: String,
)
