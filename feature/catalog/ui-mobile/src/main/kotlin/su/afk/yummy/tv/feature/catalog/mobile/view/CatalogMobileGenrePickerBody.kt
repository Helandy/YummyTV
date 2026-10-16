package su.afk.yummy.tv.feature.catalog.mobile.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.domain.catalog.model.SearchFilterOptions
import su.afk.yummy.tv.feature.catalog.model.GenrePickerMode
import su.afk.yummy.tv.feature.catalog.presentation.R

@Composable
internal fun ColumnScope.CatalogMobileGenrePickerBody(
    mode: GenrePickerMode,
    selectedIds: Set<String>,
    filterOptions: SearchFilterOptions,
    onGenreToggled: (String) -> Unit,
    onBack: () -> Unit,
) {
    SheetHeader(
        title = when (mode) {
            GenrePickerMode.INCLUDE -> stringResource(R.string.search_filter_genres)
            GenrePickerMode.EXCLUDE -> stringResource(R.string.search_filter_exclude_genres)
        },
        onBack = onBack,
    )
    Text(
        text = stringResource(R.string.search_filter_selected_count, selectedIds.size),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )

    val genresByGroup = remember(filterOptions.genres) { filterOptions.genres.groupBy { it.groupId } }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f)
            .verticalScroll(rememberScrollState())
            .padding(bottom = 4.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        filterOptions.genreGroups.forEach { group ->
            val genres = genresByGroup[group.id].orEmpty()
            if (genres.isNotEmpty()) {
                CatalogMobileGenreGroup(
                    title = group.title,
                    genres = genres,
                    selectedIds = selectedIds,
                    onGenreToggled = onGenreToggled,
                )
            }
        }

        val ungroupedGenres = remember(filterOptions.genres, filterOptions.genreGroups) {
            val groupIds = filterOptions.genreGroups.mapTo(HashSet()) { it.id }
            filterOptions.genres.filter { genre -> genre.groupId !in groupIds }
        }
        if (ungroupedGenres.isNotEmpty()) {
            CatalogMobileGenreGroup(
                title = stringResource(R.string.search_filter_genre_screen_title),
                genres = ungroupedGenres,
                selectedIds = selectedIds,
                onGenreToggled = onGenreToggled,
            )
        }
    }
}
