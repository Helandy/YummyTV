package su.afk.yummy.tv.feature.player.common

import su.afk.yummy.tv.feature.player.PlayerProgressSnapshot

data class PlayerProgressSource(
    val episodeUrl: String,
    val episode: String,
    val videoId: Int,
    val playerName: String,
    val dubbing: String,
    val screenshotUrl: String,
)

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
