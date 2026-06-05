package su.afk.yummy.tv.feature.details.episodes.utils

import su.afk.yummy.tv.core.storage.watchprogress.WatchProgressEntry
import su.afk.yummy.tv.domain.anime.model.AnimeVideo
import su.afk.yummy.tv.feature.details.episodes.model.EpisodeMobileWatchStatus

private const val MinProgressPositionMs = 30_000L
private const val WatchedProgress = 0.90f

internal fun List<AnimeVideo>.mobileWatchStatus(
    watchProgress: Map<String, WatchProgressEntry>,
): EpisodeMobileWatchStatus {
    val best = mapNotNull { watchProgress[it.iframeUrl] }
        .maxByOrNull { it.positionMs }
        ?: return EpisodeMobileWatchStatus.None

    if (best.durationMs <= 0 || best.positionMs < MinProgressPositionMs) {
        return EpisodeMobileWatchStatus.None
    }

    val progress = (best.positionMs.toFloat() / best.durationMs.toFloat()).coerceIn(0f, 1f)
    return if (progress > WatchedProgress) {
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
