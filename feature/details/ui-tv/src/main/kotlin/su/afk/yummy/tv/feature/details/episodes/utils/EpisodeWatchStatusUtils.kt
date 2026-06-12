package su.afk.yummy.tv.feature.details.episodes.utils

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import su.afk.yummy.tv.core.storage.watchprogress.WatchProgressEntry
import su.afk.yummy.tv.core.storage.watchprogress.WatchProgressStore
import su.afk.yummy.tv.domain.anime.model.AnimeVideo
import su.afk.yummy.tv.feature.details.R
import su.afk.yummy.tv.feature.details.episodes.model.EpisodeWatchStatus

internal fun List<AnimeVideo>.watchStatus(
    watchProgress: Map<String, WatchProgressEntry>,
): EpisodeWatchStatus {
    val best = mapNotNull { watchProgress[it.iframeUrl] }
        .maxByOrNull { it.positionMs }
        ?: return EpisodeWatchStatus.None

    if (!WatchProgressStore.isMeaningfulProgressEntry(best)) {
        return EpisodeWatchStatus.None
    }

    val progress = WatchProgressStore.progress(best.positionMs, best.durationMs)
    return if (WatchProgressStore.isWatchedProgressEntry(best)) {
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

@Composable
internal fun EpisodeWatchStatus.timingLabel(): String? {
    val positionMs: Long
    val durationMs: Long
    when (this) {
        EpisodeWatchStatus.None -> return null
        is EpisodeWatchStatus.InProgress -> {
            positionMs = this.positionMs
            durationMs = this.durationMs
        }

        is EpisodeWatchStatus.Watched -> {
            positionMs = this.positionMs
            durationMs = this.durationMs
        }
    }
    return stringResource(
        R.string.details_episode_watch_timing,
        positionMs.toWatchTimeString(),
        durationMs.toWatchTimeString(),
    )
}

private fun Long.toWatchTimeString(): String {
    val totalSec = coerceAtLeast(0L) / 1000L
    val h = totalSec / 3600L
    val m = (totalSec % 3600L) / 60L
    val s = totalSec % 60L
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
}
