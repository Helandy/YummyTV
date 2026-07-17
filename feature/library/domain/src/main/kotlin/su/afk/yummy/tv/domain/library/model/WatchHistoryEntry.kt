package su.afk.yummy.tv.domain.library.model

data class WatchHistoryEntry(
    val animeId: Int,
    val videoId: Int,
    val animeUrl: String,
    val title: String,
    val episode: String,
    val episodeTitle: String,
    val posterUrl: String?,
    val screenshotUrl: String?,
    val watchedAtSeconds: Long,
    val positionSeconds: Int,
    val durationSeconds: Int,
    val dubbing: String?,
    val player: String?,
)
