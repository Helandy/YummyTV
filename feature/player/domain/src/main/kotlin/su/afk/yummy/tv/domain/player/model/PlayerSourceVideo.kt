package su.afk.yummy.tv.domain.player.model

data class PlayerSourceVideo(
    val id: Int,
    val episode: String,
    val dubbing: String,
    val player: String,
    val playerId: Int? = null,
    val iframeUrl: String,
    val views: Int? = null,
    val skips: PlayerSourceSkips = PlayerSourceSkips.Empty,
)
