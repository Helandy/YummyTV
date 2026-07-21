package su.afk.yummy.tv.domain.watching.usecase

import su.afk.yummy.tv.core.model.anime.AnimeVideo
import su.afk.yummy.tv.core.model.anime.utils.isPlaceholderEpisode
import su.afk.yummy.tv.domain.player.isSupportedPlayerUrl
import su.afk.yummy.tv.domain.watching.mapper.toContinueWatchingPlaybackVideo
import javax.inject.Inject

internal class ContinueWatchingTargetResolver @Inject constructor() {

    fun resolve(
        progressVideo: ContinueWatchingPlaybackVideo,
        availableVideos: List<AnimeVideo>,
    ): ContinueWatchingPlaybackVideo {
        val candidates = availableVideos.map(AnimeVideo::toContinueWatchingPlaybackVideo)
        val exact = candidates.findExactMatch(progressVideo)
        if (exact?.iframeUrl?.isSupportedPlayerUrl() == true) return exact

        val targetEpisode = exact?.episode?.takeUnless { it.isPlaceholderEpisode() }
            ?: progressVideo.episode.takeUnless { it.isPlaceholderEpisode() }
        val sameEpisode = targetEpisode?.let { episode ->
            candidates.filter { it.episode == episode }
        }.orEmpty()
        return sameEpisode.firstOrNull { it.iframeUrl.isSupportedPlayerUrl() }
            ?: candidates.firstOrNull { it.iframeUrl.isSupportedPlayerUrl() }
            ?: exact
            ?: progressVideo
    }

    private companion object {
        fun List<ContinueWatchingPlaybackVideo>.findExactMatch(
            progressVideo: ContinueWatchingPlaybackVideo,
        ): ContinueWatchingPlaybackVideo? =
            firstOrNull { progressVideo.id > 0 && it.id == progressVideo.id }
                ?: firstOrNull {
                    progressVideo.iframeUrl.isNotBlank() &&
                            it.iframeUrl == progressVideo.iframeUrl
                }
                ?: firstOrNull {
                    !progressVideo.episode.isPlaceholderEpisode() &&
                            it.episode == progressVideo.episode &&
                            it.player == progressVideo.player &&
                            it.dubbing == progressVideo.dubbing
                }
                ?: firstOrNull {
                    !progressVideo.episode.isPlaceholderEpisode() &&
                            it.episode == progressVideo.episode
                }
    }
}
