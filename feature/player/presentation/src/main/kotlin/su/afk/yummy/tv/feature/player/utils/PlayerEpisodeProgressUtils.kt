package su.afk.yummy.tv.feature.player.utils

import su.afk.yummy.tv.core.common.episode.episodeGroupKey
import su.afk.yummy.tv.core.model.anime.AnimeWatchProgress
import su.afk.yummy.tv.core.model.anime.isMeaningfulProgress
import su.afk.yummy.tv.core.model.anime.isWatchedProgress
import su.afk.yummy.tv.core.model.anime.mergeEpisodeProgress
import su.afk.yummy.tv.core.model.anime.progress
import su.afk.yummy.tv.core.model.anime.serverWatchProgressOf
import su.afk.yummy.tv.core.utils.formatting.millisToClockTime
import su.afk.yummy.tv.feature.player.PlayerSourceGraph
import su.afk.yummy.tv.feature.player.model.PlayerEpisodeProgress

/**
 * Прогресс серий тайтла как в списке «Эпизоды»: серверный (с других устройств, из видео графа)
 * слит с локальным по общему правилу [mergeEpisodeProgress].
 */
internal fun PlayerSourceGraph.mergedWatchProgress(
    animeId: Int,
    local: List<AnimeWatchProgress>,
): List<AnimeWatchProgress> {
    val server = balancers.flatMap { balancer ->
        balancer.dubbings.flatMap { dubbing ->
            dubbing.episodes.mapNotNull { episode ->
                serverWatchProgressOf(
                    animeId = animeId,
                    episode = episode.number,
                    videoId = episode.id,
                    episodeUrl = episode.iframeUrl,
                    watchedEndTimeSeconds = episode.watchedEndTimeSeconds,
                    watchedDateSeconds = episode.watchedDateSeconds,
                    durationSeconds = episode.durationSeconds,
                    playerName = balancer.name,
                    dubbing = dubbing.name,
                )
            }
        }
    }
    return mergeEpisodeProgress(serverEntries = server, localEntries = local)
}

/** Прогресс серии [episode] среди [merged]; номера сравниваются по `episodeGroupKey`. */
internal fun List<AnimeWatchProgress>.episodeProgress(episode: String): PlayerEpisodeProgress {
    val key = episode.episodeGroupKey()
    val best = filter { it.episode.episodeGroupKey() == key && it.isMeaningfulProgress() }
        .maxByOrNull { it.updatedAt }
        ?: return PlayerEpisodeProgress.None
    return if (best.isWatchedProgress()) {
        PlayerEpisodeProgress.Watched(best.durationMs.millisToClockTime())
    } else {
        PlayerEpisodeProgress.InProgress(
            label = "${best.positionMs.millisToClockTime()} / ${best.durationMs.millisToClockTime()}",
            progress = best.progress(),
        )
    }
}
