package su.afk.yummy.tv.feature.search.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.domain.search.model.SearchFilterOptions
import su.afk.yummy.tv.domain.search.model.SearchGenre
import su.afk.yummy.tv.feature.search.R

@Composable
internal fun GenresSection(
    title: String,
    selectedIds: Set<String>,
    filterOptions: SearchFilterOptions,
    onGenreToggled: (String) -> Unit,
) {
    FilterSection(title = title) {
        val genresByGroup = filterOptions.genres.groupBy { it.groupId }
        filterOptions.genreGroups.forEach { group ->
            val genres = genresByGroup[group.id].orEmpty()
            if (genres.isNotEmpty()) {
                Text(
                    text = group.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
                ChipFlow {
                    genres.forEach { genre ->
                        GenreRow(
                            genre = genre,
                            selected = genre.id in selectedIds,
                            onClick = { onGenreToggled(genre.id) },
                        )
                    }
                }
            }
        }
        val ungroupedGenres = filterOptions.genres
            .filter { genre -> filterOptions.genreGroups.none { it.id == genre.groupId } }
        if (ungroupedGenres.isNotEmpty()) {
            ChipFlow {
                ungroupedGenres.forEach { genre ->
                    GenreRow(
                        genre = genre,
                        selected = genre.id in selectedIds,
                        onClick = { onGenreToggled(genre.id) },
                    )
                }
            }
        }
    }
}

@Composable
internal fun GenrePickerScreen(
    title: String,
    selectedIds: Set<String>,
    filterOptions: SearchFilterOptions,
    onGenreToggled: (String) -> Unit,
    backFocusRequester: FocusRequester,
    onBack: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = stringResource(R.string.search_filter_selected_count, selectedIds.size),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        SelectableRow(
            label = stringResource(R.string.search_filters_back),
            selected = false,
            onClick = onBack,
            modifier = Modifier.focusRequester(backFocusRequester),
        )
    }
    GenresSection(
        title = stringResource(R.string.search_filter_genre_screen_title),
        selectedIds = selectedIds,
        filterOptions = filterOptions,
        onGenreToggled = onGenreToggled,
    )
}

@Composable
private fun GenreRow(
    genre: SearchGenre,
    selected: Boolean,
    onClick: () -> Unit,
) {
    SelectableRow(
        label = genre.title,
        selected = selected,
        onClick = onClick,
    )
}
