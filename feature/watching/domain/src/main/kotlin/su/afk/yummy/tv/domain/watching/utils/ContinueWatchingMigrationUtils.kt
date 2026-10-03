package su.afk.yummy.tv.domain.watching.utils

import su.afk.yummy.tv.core.model.watching.ContinueWatchingPlaybackVideo
import su.afk.yummy.tv.core.utils.episode.isPlaceholderEpisode
import su.afk.yummy.tv.domain.home.model.ContinueWatchingProgressMigration
import su.afk.yummy.tv.domain.home.model.HomeContinueWatchingItem

/** Миграция прогресса с placeholder-серии на реальную; null, если цели нельзя доверять. */
internal fun createContinueWatchingProgressMigration(
    entry: HomeContinueWatchingItem,
    progressVideo: ContinueWatchingPlaybackVideo,
    targetVideo: ContinueWatchingPlaybackVideo,
    posterUrl: String,
): ContinueWatchingProgressMigration? {
    if (!progressVideo.isTrustedMigrationTarget(targetVideo)) return null
    return ContinueWatchingProgressMigration(
        animeId = entry.animeId,
        previousEpisode = entry.episode,
        episode = targetVideo.episode,
        videoId = targetVideo.id,
        episodeUrl = targetVideo.iframeUrl,
        positionMs = entry.positionMs,
        durationMs = entry.durationMs,
        animeTitle = entry.animeTitle,
        posterUrl = posterUrl,
        playerName = targetVideo.player,
        dubbing = targetVideo.dubbing,
        screenshotUrl = entry.screenshotUrl,
    )
}

private fun ContinueWatchingPlaybackVideo.isTrustedMigrationTarget(
    targetVideo: ContinueWatchingPlaybackVideo,
): Boolean {
    if (!episode.isPlaceholderEpisode() || targetVideo.episode.isPlaceholderEpisode()) {
        return false
    }
    if (episode == targetVideo.episode) return false
    return (id > 0 && targetVideo.id == id) ||
            (iframeUrl.isNotBlank() && targetVideo.iframeUrl == iframeUrl)
}
