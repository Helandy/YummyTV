package su.afk.yummy.tv.feature.search.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.domain.search.model.SearchFilterOptions
import su.afk.yummy.tv.feature.search.mobile.R
import su.afk.yummy.tv.feature.search.mobile.model.GenrePickerMode

@Composable
internal fun SearchMobileGenrePickerBody(
    mode: GenrePickerMode,
    selectedIds: Set<String>,
    filterOptions: SearchFilterOptions,
    onGenreToggled: (String) -> Unit,
    onBack: () -> Unit,
) {
    SheetHeader(
        title = when (mode) {
            GenrePickerMode.INCLUDE -> stringResource(R.string.search_mobile_filter_genres)
            GenrePickerMode.EXCLUDE -> stringResource(R.string.search_mobile_filter_exclude_genres)
        },
        actionLabel = stringResource(R.string.search_mobile_filters_back),
        onClose = onBack,
    )
    Text(
        text = stringResource(R.string.search_mobile_filter_selected_count, selectedIds.size),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )

    val genresByGroup = filterOptions.genres.groupBy { it.groupId }
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 560.dp),
        contentPadding = PaddingValues(bottom = 4.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        filterOptions.genreGroups.forEach { group ->
            val genres = genresByGroup[group.id].orEmpty()
            if (genres.isNotEmpty()) {
                item(key = "group:${group.id}") {
                    SearchMobileGenreGroup(
                        title = group.title,
                        genres = genres,
                        selectedIds = selectedIds,
                        onGenreToggled = onGenreToggled,
                    )
                }
            }
        }

        val ungroupedGenres = filterOptions.genres
            .filter { genre -> filterOptions.genreGroups.none { it.id == genre.groupId } }
        if (ungroupedGenres.isNotEmpty()) {
            item(key = "ungrouped") {
                SearchMobileGenreGroup(
                    title = stringResource(R.string.search_mobile_filter_genre_screen_title),
                    genres = ungroupedGenres,
                    selectedIds = selectedIds,
                    onGenreToggled = onGenreToggled,
                )
            }
        }
    }
}
