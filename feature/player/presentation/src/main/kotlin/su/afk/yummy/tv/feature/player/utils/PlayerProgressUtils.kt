package su.afk.yummy.tv.feature.player.utils

import su.afk.yummy.tv.core.storage.watchprogress.WatchProgressStore
import su.afk.yummy.tv.feature.player.PlayerProgressSnapshot
import su.afk.yummy.tv.feature.player.PlayerState
import su.afk.yummy.tv.feature.player.handler.PlayerProgressContext
import su.afk.yummy.tv.feature.player.model.PlayerCompletionAnalyticsKey

internal fun PlayerProgressSnapshot.withFullTimingIfWatched(): PlayerProgressSnapshot =
    if (WatchProgressStore.isWatchedProgress(positionMs, durationMs)) {
        copy(positionMs = durationMs)
    } else {
        this
    }

internal fun PlayerState.State.progressContext(): PlayerProgressContext =
    PlayerProgressContext(
        animeId = animeId,
        animeTitle = animeTitle,
        posterUrl = posterUrl,
    )

internal fun PlayerState.State.progressSnapshot(
    positionMs: Long,
    durationMs: Long,
): PlayerProgressSnapshot? {
    val episodeUrl = activeIframeUrl(this)
    val episode = activeEpisode(this)
    if (episodeUrl.isBlank() || episode.isBlank()) return null
    return PlayerProgressSnapshot(
        episode = episode,
        episodeUrl = episodeUrl,
        videoId = activeVideoId(this),
        playerName = activeBalancerName(this),
        dubbing = activeDubbingName(this),
        screenshotUrl = activeScreenshotUrl(this),
        positionMs = positionMs,
        durationMs = durationMs,
    )
}

internal fun PlayerState.State.completionAnalyticsKey(): PlayerCompletionAnalyticsKey? {
    val animeId = animeId.takeIf { it > 0 } ?: return null
    val videoId = activeVideoId(this)
    val episode = activeEpisode(this)
    val iframeUrl = activeIframeUrl(this)
    if (videoId <= 0 && episode.isBlank() && iframeUrl.isBlank()) return null
    return PlayerCompletionAnalyticsKey(
        animeId = animeId,
        videoId = videoId,
        episode = episode,
        iframeUrl = iframeUrl,
    )
}

internal fun String.isFirstEpisodeNumber(): Boolean {
    val normalized = trim().replace(',', '.')
    val number = normalized.toDoubleOrNull()
        ?: Regex("""\d+(?:[.,]\d+)?""")
            .find(normalized)
            ?.value
            ?.replace(',', '.')
            ?.toDoubleOrNull()
    return number == 1.0
}
