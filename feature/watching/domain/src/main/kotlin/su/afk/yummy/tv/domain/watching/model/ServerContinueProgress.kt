package su.afk.yummy.tv.domain.watching.model

import su.afk.yummy.tv.core.model.watching.ContinueWatchingPlaybackVideo

internal data class ServerContinueProgress(
    val video: ContinueWatchingPlaybackVideo,
    val positionMs: Long,
    val updatedAt: Long,
)
