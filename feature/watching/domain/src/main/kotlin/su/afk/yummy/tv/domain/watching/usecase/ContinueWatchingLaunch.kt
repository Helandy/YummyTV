package su.afk.yummy.tv.domain.watching.usecase

import su.afk.yummy.tv.core.model.anime.AnimeVideoSkips
import su.afk.yummy.tv.domain.home.model.ContinueWatchingProgressMigration

data class ContinueWatchingLaunch(
    val animeId: Int,
    val animeTitle: String,
    val posterUrl: String,
    val video: ContinueWatchingPlaybackVideo,
    val resumeFromMs: Long,
    val remoteProgressSwitch: ContinueWatchingRemoteProgressSwitch? = null,
)

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

data class ContinueWatchingRemoteProgressSwitch(
    val episode: String,
    val positionMs: Long,
)

internal data class ContinueWatchingLaunchResolution(
    val launch: ContinueWatchingLaunch,
    val progressMigration: ContinueWatchingProgressMigration? = null,
)
