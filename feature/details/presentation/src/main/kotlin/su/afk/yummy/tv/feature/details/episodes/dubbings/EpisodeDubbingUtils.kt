package su.afk.yummy.tv.feature.details.episodes.dubbings

import su.afk.yummy.tv.core.model.anime.AnimeVideo
import su.afk.yummy.tv.core.model.settings.PreferredPlayer
import su.afk.yummy.tv.core.utils.episode.episodeGroupKey
import su.afk.yummy.tv.feature.details.utils.matchesPreferredPlayer
import su.afk.yummy.tv.feature.player.isSupportedPlayerUrl
import su.afk.yummy.tv.feature.player.playerDisplayOrderPriority

internal fun List<AnimeVideo>.selectEpisodeDubbingLaunchVideo(
    episode: String,
    dubbingName: String,
    preferredPlayer: PreferredPlayer,
): AnimeVideo? {
    val candidates = filter { it.isEpisodeDubbing(episode, dubbingName) }
    val supported = candidates.filter { it.iframeUrl.isSupportedPlayerUrl() }
    return supported.firstOrNull { it.iframeUrl.matchesPreferredPlayer(preferredPlayer) }
        ?: supported.minWithOrNull(
            compareBy<AnimeVideo> {
                minOf(
                    it.player.playerDisplayOrderPriority(),
                    it.iframeUrl.playerDisplayOrderPriority(),
                )
            }.thenBy { it.player }
        )
        ?: candidates.firstOrNull()
}

/** Видео той же серии (номера сравниваются через [episodeGroupKey]) в озвучке [dubbingName]. */
internal fun AnimeVideo.isEpisodeDubbing(episode: String, dubbingName: String): Boolean =
    this.episode.episodeGroupKey() == episode.episodeGroupKey() && dubbing.trim() == dubbingName

/** Все видео той же серии и озвучки, что и [video], — кандидаты для выбора балансера. */
internal fun List<AnimeVideo>.sameEpisodeDubbing(video: AnimeVideo): List<AnimeVideo> =
    filter { it.isEpisodeDubbing(video.episode, video.dubbing.trim()) }
