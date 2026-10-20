package su.afk.yummy.tv.domain.watching.utils

import su.afk.yummy.tv.core.common.episode.isPlaceholderEpisode
import su.afk.yummy.tv.core.common.player.isExtraSourceUrl
import su.afk.yummy.tv.core.common.player.isSupportedPlayerUrl
import su.afk.yummy.tv.core.model.anime.AnimeVideo
import su.afk.yummy.tv.core.model.watching.ContinueWatchingPlaybackVideo

/** Подбирает видео, с которого лучше продолжить: точное совпадение или ближайшее рабочее. */
internal fun resolveContinueWatchingTarget(
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

private fun List<ContinueWatchingPlaybackVideo>.findExactMatch(
    progressVideo: ContinueWatchingPlaybackVideo,
): ContinueWatchingPlaybackVideo? =
    // Прогресс 4K хранит id настоящего видео-двойника (под ним он уходит на сервер), поэтому
    // совпадение по id открыло бы балансер-двойник вместо 4K: для extra-source ищем сперва по URL.
    firstOrNull {
        progressVideo.iframeUrl.isExtraSourceUrl() && it.iframeUrl == progressVideo.iframeUrl
    }
        ?: firstOrNull { progressVideo.id > 0 && it.id == progressVideo.id }
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
