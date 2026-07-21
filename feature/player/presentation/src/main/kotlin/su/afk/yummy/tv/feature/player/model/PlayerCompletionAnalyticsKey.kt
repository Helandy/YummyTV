package su.afk.yummy.tv.feature.player.model

internal data class PlayerCompletionAnalyticsKey(
    val animeId: Int,
    val videoId: Int,
    val episode: String,
    val iframeUrl: String,
)
