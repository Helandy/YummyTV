package su.afk.yummy.tv.feature.player.utils

import su.afk.yummy.tv.feature.player.PlayerProgressSnapshot

internal fun buildTvProgressSnapshot(
    episodeKey: String,
    episode: String,
    videoId: Int,
    playerName: String,
    dubbing: String,
    screenshotUrl: String,
    positionMs: Long,
    durationMs: Long,
): PlayerProgressSnapshot? {
    if (episodeKey.isBlank() || durationMs <= 0) return null
    return PlayerProgressSnapshot(
        episode = episode,
        episodeUrl = episodeKey,
        videoId = videoId,
        playerName = playerName,
        dubbing = dubbing,
        screenshotUrl = screenshotUrl,
        positionMs = positionMs,
        durationMs = durationMs,
    )
}
