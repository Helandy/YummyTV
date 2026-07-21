package su.afk.yummy.tv.feature.player.common.utils

import su.afk.yummy.tv.feature.player.PlayerProgressSnapshot
import su.afk.yummy.tv.feature.player.common.model.PlayerProgressSource

fun PlayerProgressSource.buildProgressSnapshot(
    positionMs: Long,
    durationMs: Long,
): PlayerProgressSnapshot? {
    if (episodeUrl.isBlank() || durationMs <= 0) return null
    return PlayerProgressSnapshot(
        episode = episode,
        episodeUrl = episodeUrl,
        videoId = videoId,
        playerName = playerName,
        dubbing = dubbing,
        screenshotUrl = screenshotUrl,
        positionMs = positionMs,
        durationMs = durationMs,
    )
}
