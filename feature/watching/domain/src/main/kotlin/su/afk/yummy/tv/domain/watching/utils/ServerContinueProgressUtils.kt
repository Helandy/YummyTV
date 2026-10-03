package su.afk.yummy.tv.domain.watching.utils

import su.afk.yummy.tv.core.model.anime.AnimeVideo
import su.afk.yummy.tv.core.model.anime.isMeaningfulProgress
import su.afk.yummy.tv.core.model.anime.isWatchedProgress
import su.afk.yummy.tv.core.utils.episode.episodeNumberOrNull
import su.afk.yummy.tv.domain.watching.model.ServerContinueProgress

/** Самый свежий серверный прогресс среди видео тайтла, который стоит предлагать к продолжению. */
internal fun List<AnimeVideo>.selectServerContinueProgress(): ServerContinueProgress? =
    mapNotNull { video -> video.toServerProgress() }
        .maxWithOrNull(
            compareBy<ServerContinueProgress> { it.updatedAt }
                .thenBy(ServerContinueProgress::positionMs)
                .thenBy {
                    it.video.episode.episodeNumberOrNull() ?: Double.NEGATIVE_INFINITY
                },
        )

private fun AnimeVideo.toServerProgress(): ServerContinueProgress? {
    val positionSeconds = watchedEndTimeSeconds
        ?.takeIf { it >= 0 }
        ?: return null
    val updatedAtSeconds = watchedDateSeconds
        ?.takeIf { it > 0L }
        ?: return null
    val durationSeconds = durationSeconds
        ?.takeIf { it > 0 }
        ?: return null
    val positionMs = positionSeconds * 1_000L
    val durationMs = durationSeconds * 1_000L
    if (!isMeaningfulProgress(positionMs, durationMs)) return null
    if (isWatchedProgress(positionMs, durationMs)) return null
    return ServerContinueProgress(
        video = toContinueWatchingPlaybackVideo(),
        positionMs = positionMs,
        updatedAt = updatedAtSeconds * 1_000L,
    )
}
