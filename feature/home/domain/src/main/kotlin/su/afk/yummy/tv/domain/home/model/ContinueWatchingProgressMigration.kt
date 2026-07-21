package su.afk.yummy.tv.domain.home.model

/** Replacement source for a legacy placeholder continue-watching entry. */
data class ContinueWatchingProgressMigration(
    val animeId: Int,
    val previousEpisode: String,
    val episode: String,
    val videoId: Int,
    val episodeUrl: String,
    val positionMs: Long,
    val durationMs: Long,
    val animeTitle: String,
    val posterUrl: String,
    val playerName: String,
    val dubbing: String,
    val screenshotUrl: String,
)
