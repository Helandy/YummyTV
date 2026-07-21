package su.afk.yummy.tv.domain.player.model

data class PlayerSourceData(
    val videos: List<PlayerSourceVideo>,
    val screenshotByEpisode: Map<String, String> = emptyMap(),
)
