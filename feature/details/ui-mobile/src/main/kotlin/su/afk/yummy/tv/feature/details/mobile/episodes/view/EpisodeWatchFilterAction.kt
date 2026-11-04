package su.afk.yummy.tv.feature.details.mobile.episodes.view

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import su.afk.yummy.tv.feature.details.episodes.model.EpisodeWatchFilter
import su.afk.yummy.tv.feature.details.presentation.R

/** Иконка фильтра в топ-баре: меню «Все / Непросмотренные / Просмотренные». */
@Composable
internal fun EpisodeWatchFilterAction(
    selected: EpisodeWatchFilter,
    onSelected: (EpisodeWatchFilter) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val active = selected != EpisodeWatchFilter.All
    IconButton(onClick = { expanded = true }) {
        Icon(
            imageVector = Icons.Filled.FilterList,
            contentDescription = stringResource(selected.labelRes()),
            tint = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
        EpisodeWatchFilter.entries.forEach { filter ->
            DropdownMenuItem(
                text = {
                    Text(
                        text = stringResource(filter.labelRes()),
                        style = MaterialTheme.typography.titleSmall,
                        color = if (filter == selected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                    )
                },
                onClick = {
                    expanded = false
                    onSelected(filter)
                },
            )
        }
    }
}

private fun EpisodeWatchFilter.labelRes(): Int = when (this) {
    EpisodeWatchFilter.All -> R.string.details_episodes_filter_all
    EpisodeWatchFilter.Unwatched -> R.string.details_episodes_filter_unwatched
    EpisodeWatchFilter.Watched -> R.string.details_episodes_filter_watched
}
