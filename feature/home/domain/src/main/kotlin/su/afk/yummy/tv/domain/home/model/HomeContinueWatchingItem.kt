package su.afk.yummy.tv.domain.home.model

data class HomeContinueWatchingItem(
    val animeId: Int,
    val animeTitle: String,
    val description: String,
    val poster: HomePoster?,
    val videoId: Int,
    val episode: String,
    val episodeUrl: String,
    val positionMs: Long,
    val durationMs: Long,
    val updatedAt: Long,
    val playerName: String,
    val dubbing: String,
    val screenshotUrl: String,
)
