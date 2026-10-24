package su.afk.yummy.tv.domain.home.model

import su.afk.yummy.tv.core.model.anime.AnimePoster

data class HomeContinueWatchingItem(
    val animeId: Int,
    val animeTitle: String,
    val description: String,
    val poster: AnimePoster?,
    val videoId: Int,
    val episode: String,
    val episodeUrl: String,
    val positionMs: Long,
    val durationMs: Long,
    val updatedAt: Long,
    val playerName: String,
    val dubbing: String,
    val screenshotUrl: String,
    val source: ContinueWatchingSource = ContinueWatchingSource.LOCAL,
)
