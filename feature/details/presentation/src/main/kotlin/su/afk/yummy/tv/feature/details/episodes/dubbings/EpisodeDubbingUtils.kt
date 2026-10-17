package su.afk.yummy.tv.feature.details.episodes.dubbings

import su.afk.yummy.tv.core.common.episode.episodeGroupKey
import su.afk.yummy.tv.core.common.player.isSupportedPlayerUrl
import su.afk.yummy.tv.core.common.player.balancerDisplayComparator
import su.afk.yummy.tv.core.model.anime.AnimeVideo
import su.afk.yummy.tv.core.model.settings.PreferredPlayer
import su.afk.yummy.tv.feature.details.utils.matchesPreferredPlayer

internal fun List<AnimeVideo>.selectEpisodeDubbingLaunchVideo(
    episode: String,
    dubbingName: String,
    preferredPlayer: PreferredPlayer,
): AnimeVideo? {
    val viewsByPlayer = groupingBy { it.player }.fold(0) { sum, video -> sum + (video.views ?: 0) }
    val candidates = filter { it.isEpisodeDubbing(episode, dubbingName) }
    val supported = candidates.filter { it.iframeUrl.isSupportedPlayerUrl() }
    return supported.firstOrNull { it.iframeUrl.matchesPreferredPlayer(preferredPlayer) }
        ?: supported.minWithOrNull(
            balancerDisplayComparator<AnimeVideo>(
                playerName = { it.player },
                iframeUrl = { it.iframeUrl },
                views = { video -> viewsByPlayer[video.player] ?: 0 },
            ),
        )
        ?: candidates.firstOrNull()
}

/** Видео той же серии (номера сравниваются через [episodeGroupKey]) в озвучке [dubbingName]. */
internal fun AnimeVideo.isEpisodeDubbing(episode: String, dubbingName: String): Boolean =
    this.episode.episodeGroupKey() == episode.episodeGroupKey() && dubbing.trim() == dubbingName

/** Все видео той же серии и озвучки, что и [video], — кандидаты для выбора балансера. */
internal fun List<AnimeVideo>.sameEpisodeDubbing(video: AnimeVideo): List<AnimeVideo> =
    filter { it.isEpisodeDubbing(video.episode, video.dubbing.trim()) }
