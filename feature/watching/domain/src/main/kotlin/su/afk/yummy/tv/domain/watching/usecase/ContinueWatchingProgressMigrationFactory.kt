package su.afk.yummy.tv.domain.watching.usecase

import su.afk.yummy.tv.domain.home.model.ContinueWatchingProgressMigration
import su.afk.yummy.tv.domain.home.model.HomeContinueWatchingItem
import su.afk.yummy.tv.core.model.anime.utils.isPlaceholderEpisode
import javax.inject.Inject

internal class ContinueWatchingProgressMigrationFactory @Inject constructor() {

    fun create(
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

    private companion object {
        fun ContinueWatchingPlaybackVideo.isTrustedMigrationTarget(
            targetVideo: ContinueWatchingPlaybackVideo,
        ): Boolean {
            if (!episode.isPlaceholderEpisode() || targetVideo.episode.isPlaceholderEpisode()) {
                return false
            }
            if (episode == targetVideo.episode) return false
            return (id > 0 && targetVideo.id == id) ||
                    (iframeUrl.isNotBlank() && targetVideo.iframeUrl == iframeUrl)
        }
    }
}
