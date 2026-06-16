package su.afk.yummy.tv.domain.anime.model

data class AnimeVideo(
    val id: Int,
    val episode: String,
    val dubbing: String,
    val player: String,
    val playerId: Int?,
    val iframeUrl: String,
    val durationSeconds: Int?,
    val views: Int? = null,
    val watchedEndTimeSeconds: Int? = null,
    val watchedDateSeconds: Long? = null,
    val skips: AnimeVideoSkips = AnimeVideoSkips(),
)

data class AnimeVideoSkips(
    val opening: AnimeVideoSkipSegment? = null,
    val ending: AnimeVideoSkipSegment? = null,
)

data class AnimeVideoSkipSegment(
    val startMs: Long,
    val endMs: Long,
)
