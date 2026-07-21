package su.afk.yummy.tv.domain.watching.usecase

import su.afk.yummy.tv.domain.home.model.ContinueWatchingProgressMigration

data class ContinueWatchingLaunch(
    val animeId: Int,
    val animeTitle: String,
    val posterUrl: String,
    val video: ContinueWatchingPlaybackVideo,
    val resumeFromMs: Long,
    val remoteProgressSwitch: ContinueWatchingRemoteProgressSwitch? = null,
)

internal data class ContinueWatchingLaunchResolution(
    val launch: ContinueWatchingLaunch,
    val progressMigration: ContinueWatchingProgressMigration? = null,
)
