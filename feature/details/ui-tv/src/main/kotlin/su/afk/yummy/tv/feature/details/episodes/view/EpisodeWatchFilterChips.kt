package su.afk.yummy.tv.feature.details.episodes.view

import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.core.designsystem.tv.TvChip
import su.afk.yummy.tv.feature.details.episodes.model.EpisodeWatchFilter
import su.afk.yummy.tv.feature.details.episodes.utils.tvChipLabelRes

/** Ряд чипов фильтра серий по статусу просмотра. */
@Composable
internal fun EpisodeWatchFilterChips(
    selected: EpisodeWatchFilter,
    onSelected: (EpisodeWatchFilter) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.focusGroup(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        EpisodeWatchFilter.entries.forEach { filter ->
            TvChip(
                label = stringResource(filter.tvChipLabelRes()),
                selected = filter == selected,
                onClick = { onSelected(filter) },
            )
        }
    }
}
