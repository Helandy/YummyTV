package su.afk.yummy.tv.domain.watching.usecase

import su.afk.yummy.tv.core.model.anime.AnimeVideoSkips

data class ContinueWatchingPlaybackVideo(
    val id: Int,
    val episode: String,
    val dubbing: String,
    val player: String,
    val playerId: Int? = null,
    val iframeUrl: String,
    val views: Int? = null,
    val skips: AnimeVideoSkips = AnimeVideoSkips(),
)
