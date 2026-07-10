package su.afk.yummy.tv.domain.player.model

data class PlayerStreamRequest(
    val iframeUrl: String,
    val autoQualityLabel: String,
    val sessionFallbackTtlSeconds: Int? = null,
    val reusePlaybackSession: Boolean = true,
)
