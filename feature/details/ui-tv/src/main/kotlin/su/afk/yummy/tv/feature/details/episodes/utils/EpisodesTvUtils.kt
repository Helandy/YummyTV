package su.afk.yummy.tv.feature.details.episodes.utils

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import su.afk.yummy.tv.feature.details.episodes.model.EpisodeRange
import su.afk.yummy.tv.feature.details.episodes.model.EpisodeWatchFilter
import su.afk.yummy.tv.feature.details.presentation.R

/** Подпись чипа фильтра серий на ТВ. */
@StringRes
internal fun EpisodeWatchFilter.tvChipLabelRes(): Int = when (this) {
    EpisodeWatchFilter.All -> R.string.details_episodes_filter_all
    EpisodeWatchFilter.Unwatched -> R.string.details_episodes_filter_unwatched
    EpisodeWatchFilter.Watched -> R.string.details_episodes_filter_watched
}

/** Подпись чипа диапазона серий на ТВ (`1–50`, либо одиночный номер на хвосте списка). */
@Composable
internal fun EpisodeRange.tvChipLabel(): String =
    if (firstEpisode == lastEpisode) {
        firstEpisode
    } else {
        stringResource(R.string.details_episodes_range_label, firstEpisode, lastEpisode)
    }
