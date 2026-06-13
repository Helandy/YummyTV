package su.afk.yummy.tv.feature.player.utils

import su.afk.yummy.tv.feature.player.PlayerProgressSnapshot
import su.afk.yummy.tv.feature.player.model.MobilePlayerUiState

internal fun MobilePlayerUiState.buildProgressSnapshot(
    positionMs: Long,
    durationMs: Long,
): PlayerProgressSnapshot? {
    if (durationMs <= 0 || activeIframeUrl.isBlank()) return null
    return PlayerProgressSnapshot(
        episode = activeEpisode,
        episodeUrl = activeIframeUrl,
        videoId = activeVideoId,
        playerName = activeBalancerName,
        dubbing = activeDubbing,
        screenshotUrl = activeScreenshotUrl,
        positionMs = positionMs,
        durationMs = durationMs,
    )
}
