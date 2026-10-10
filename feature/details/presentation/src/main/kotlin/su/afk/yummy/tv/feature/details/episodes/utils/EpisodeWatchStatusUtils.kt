package su.afk.yummy.tv.feature.details.episodes.utils

import su.afk.yummy.tv.core.model.anime.AnimeVideo
import su.afk.yummy.tv.core.model.anime.isMeaningfulProgress
import su.afk.yummy.tv.core.model.anime.isWatchedProgress
import su.afk.yummy.tv.core.model.anime.progress
import su.afk.yummy.tv.core.utils.formatting.millisToClockTime
import su.afk.yummy.tv.feature.details.episodes.model.EpisodeWatchStatus
import su.afk.yummy.tv.feature.details.model.DetailsWatchProgressIndex

fun List<AnimeVideo>.watchStatus(
    watchProgress: DetailsWatchProgressIndex,
): EpisodeWatchStatus {
    val best = watchProgress.bestFor(this)
        ?: return EpisodeWatchStatus.None

    if (!best.isMeaningfulProgress()) {
        return EpisodeWatchStatus.None
    }

    val progress = best.progress()
    return if (best.isWatchedProgress()) {
        EpisodeWatchStatus.Watched(
            positionMs = best.positionMs,
            durationMs = best.durationMs,
        )
    } else {
        EpisodeWatchStatus.InProgress(
            progress = progress,
            positionMs = best.positionMs,
            durationMs = best.durationMs,
        )
    }
}

/** Тайминг серии: «24:00» без прогресса и у досмотренных, «11:24 / 24:00» — у начатых. */
fun EpisodeWatchStatus.durationLabel(fallbackDurationSeconds: Int?): String? {
    val fallbackMs = fallbackDurationSeconds?.takeIf { it > 0 }?.times(1_000L)
    return when (this) {
        EpisodeWatchStatus.None -> fallbackMs?.millisToClockTime()

        is EpisodeWatchStatus.Watched ->
            (durationMs.takeIf { it > 0 } ?: fallbackMs)?.millisToClockTime()

        is EpisodeWatchStatus.InProgress -> {
            val totalMs = durationMs.takeIf { it > 0 } ?: fallbackMs
            ?: return positionMs.millisToClockTime()
            "${positionMs.millisToClockTime()} / ${totalMs.millisToClockTime()}"
        }
    }
}
