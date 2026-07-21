package su.afk.yummy.tv.domain.player.model

data class PlayerSourceEpisode(
    val id: Int = 0,
    val playerId: Int? = null,
    val number: String = "",
    val iframeUrl: String = "",
    val screenshotUrl: String = "",
    val skips: PlayerSourceSkips = PlayerSourceSkips.Empty,
)
