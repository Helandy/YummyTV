package su.afk.yummy.tv.feature.details.episodes.utils

import androidx.annotation.StringRes
import su.afk.yummy.tv.feature.details.episodes.model.EpisodeWatchFilter
import su.afk.yummy.tv.feature.details.presentation.R

/** Подпись чипа фильтра серий на ТВ. */
@StringRes
internal fun EpisodeWatchFilter.tvChipLabelRes(): Int = when (this) {
    EpisodeWatchFilter.All -> R.string.details_episodes_filter_all
    EpisodeWatchFilter.Unwatched -> R.string.details_episodes_filter_unwatched
    EpisodeWatchFilter.Watched -> R.string.details_episodes_filter_watched
}
