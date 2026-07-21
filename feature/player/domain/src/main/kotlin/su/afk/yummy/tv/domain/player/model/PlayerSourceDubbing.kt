package su.afk.yummy.tv.domain.player.model

data class PlayerSourceDubbing(
    val name: String,
    val episodes: List<PlayerSourceEpisode> = emptyList(),
    val views: Int = 0,
)
