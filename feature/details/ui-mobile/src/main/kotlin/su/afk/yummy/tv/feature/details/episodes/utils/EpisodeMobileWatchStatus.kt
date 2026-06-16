package su.afk.yummy.tv.feature.details.episodes.utils

import su.afk.yummy.tv.core.storage.watchprogress.WatchProgressStore
import su.afk.yummy.tv.domain.anime.model.AnimeVideo
import su.afk.yummy.tv.feature.details.details.DetailsWatchProgressIndex
import su.afk.yummy.tv.feature.details.episodes.model.EpisodeMobileWatchStatus

internal fun List<AnimeVideo>.mobileWatchStatus(
    watchProgress: DetailsWatchProgressIndex,
): EpisodeMobileWatchStatus {
    val best = watchProgress.bestFor(this)
        ?: return EpisodeMobileWatchStatus.None

    if (!WatchProgressStore.isMeaningfulProgressEntry(best)) {
        return EpisodeMobileWatchStatus.None
    }

    val progress = WatchProgressStore.progress(best.positionMs, best.durationMs)
    return if (WatchProgressStore.isWatchedProgressEntry(best)) {
        EpisodeMobileWatchStatus.Watched(
            positionMs = best.positionMs,
            durationMs = best.durationMs,
        )
    } else {
        EpisodeMobileWatchStatus.InProgress(
            progress = progress,
            positionMs = best.positionMs,
            durationMs = best.durationMs,
        )
    }
}

internal fun EpisodeMobileWatchStatus.timingLabel(): String? = when (this) {
    EpisodeMobileWatchStatus.None -> null
    is EpisodeMobileWatchStatus.InProgress -> "${positionMs.toMobileWatchTimeString()} / ${durationMs.toMobileWatchTimeString()}"
    is EpisodeMobileWatchStatus.Watched -> "${positionMs.toMobileWatchTimeString()} / ${durationMs.toMobileWatchTimeString()}"
}

private fun Long.toMobileWatchTimeString(): String {
    val totalSec = coerceAtLeast(0L) / 1000L
    val h = totalSec / 3600L
    val m = (totalSec % 3600L) / 60L
    val s = totalSec % 60L
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
}
