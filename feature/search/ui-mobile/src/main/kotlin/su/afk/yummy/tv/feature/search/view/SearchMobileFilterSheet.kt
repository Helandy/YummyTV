package su.afk.yummy.tv.feature.search.view

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.domain.search.model.SearchFilterOptions
import su.afk.yummy.tv.domain.search.model.SearchFilters
import su.afk.yummy.tv.domain.search.model.SearchGenre
import su.afk.yummy.tv.domain.search.model.SearchSort
import su.afk.yummy.tv.feature.search.mobile.R
import su.afk.yummy.tv.feature.search.mobile.model.GenrePickerMode
import su.afk.yummy.tv.feature.search.utils.ageOptions
import su.afk.yummy.tv.feature.search.utils.label
import su.afk.yummy.tv.feature.search.utils.seasonOptions
import su.afk.yummy.tv.feature.search.utils.statusOptions

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
internal fun SearchMobileFilterSheet(
    draftFilters: SearchFilters,
    filterOptions: SearchFilterOptions,
    isLoadingFilterOptions: Boolean,
    onClose: () -> Unit,
    onApply: () -> Unit,
    onReset: () -> Unit,
    onGenreToggled: (String) -> Unit,
    onExcludedGenreToggled: (String) -> Unit,
    onTypeToggled: (String) -> Unit,
    onStatusToggled: (String) -> Unit,
    onSeasonToggled: (String) -> Unit,
    onAgeRatingToggled: (Int) -> Unit,
    onFromYearChanged: (Int?) -> Unit,
    onToYearChanged: (Int?) -> Unit,
    onSortSelected: (SearchSort) -> Unit,
    onSortDirectionToggled: () -> Unit,
) {
    var genrePickerMode by remember { mutableStateOf<GenrePickerMode?>(null) }
    val currentGenrePickerMode = genrePickerMode

    ModalBottomSheet(onDismissRequest = onClose) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            if (currentGenrePickerMode != null) {
                GenrePickerContent(
                    mode = currentGenrePickerMode,
                    selectedIds = when (currentGenrePickerMode) {
                        GenrePickerMode.INCLUDE -> draftFilters.genres
                        GenrePickerMode.EXCLUDE -> draftFilters.excludedGenres
                    },
                    filterOptions = filterOptions,
                    onGenreToggled = when (currentGenrePickerMode) {
                        GenrePickerMode.INCLUDE -> onGenreToggled
                        GenrePickerMode.EXCLUDE -> onExcludedGenreToggled
                    },
                    onBack = { genrePickerMode = null },
                )
            } else {
                FilterContent(
                    draftFilters = draftFilters,
                    filterOptions = filterOptions,
                    isLoadingFilterOptions = isLoadingFilterOptions,
                    onApply = onApply,
                    onReset = onReset,
                    onOpenGenres = { genrePickerMode = GenrePickerMode.INCLUDE },
                    onOpenExcludedGenres = { genrePickerMode = GenrePickerMode.EXCLUDE },
                    onTypeToggled = onTypeToggled,
                    onStatusToggled = onStatusToggled,
                    onSeasonToggled = onSeasonToggled,
                    onAgeRatingToggled = onAgeRatingToggled,
                    onFromYearChanged = onFromYearChanged,
                    onToYearChanged = onToYearChanged,
                    onSortSelected = onSortSelected,
                    onSortDirectionToggled = onSortDirectionToggled,
                )
            }
            Spacer(Modifier.height(10.dp))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FilterContent(
    draftFilters: SearchFilters,
    filterOptions: SearchFilterOptions,
    isLoadingFilterOptions: Boolean,
    onApply: () -> Unit,
    onReset: () -> Unit,
    onOpenGenres: () -> Unit,
    onOpenExcludedGenres: () -> Unit,
    onTypeToggled: (String) -> Unit,
    onStatusToggled: (String) -> Unit,
    onSeasonToggled: (String) -> Unit,
    onAgeRatingToggled: (Int) -> Unit,
    onFromYearChanged: (Int?) -> Unit,
    onToYearChanged: (Int?) -> Unit,
    onSortSelected: (SearchSort) -> Unit,
    onSortDirectionToggled: () -> Unit,
) {
    SheetHeader(
        title = stringResource(R.string.search_mobile_filters),
    )

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(onClick = onReset, modifier = Modifier.weight(1f)) {
            Text(stringResource(R.string.search_mobile_filters_reset))
        }
        Button(onClick = onApply, modifier = Modifier.weight(1f)) {
            Text(stringResource(R.string.search_mobile_filters_apply))
        }
    }

    if (isLoadingFilterOptions) {
        Text(
            text = stringResource(R.string.search_mobile_filters_loading),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 560.dp),
        contentPadding = PaddingValues(bottom = 4.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            FilterSection(title = stringResource(R.string.search_mobile_filter_sort)) {
                ChipFlow {
                    SearchSort.entries.forEach { sort ->
                        FilterChip(
                            label = sort.label(),
                            selected = draftFilters.sort == sort,
                            onClick = { onSortSelected(sort) },
                        )
                    }
                    FilterChip(
                        label = if (draftFilters.sortForward) {
                            stringResource(R.string.search_mobile_filter_sort_forward)
                        } else {
                            stringResource(R.string.search_mobile_filter_sort_backward)
                        },
                        selected = !draftFilters.sortForward,
                        onClick = onSortDirectionToggled,
                    )
                }
            }
        }

        item {
            FilterSection(title = stringResource(R.string.search_mobile_filter_year)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    YearField(
                        label = stringResource(R.string.search_mobile_filter_year_from),
                        value = draftFilters.fromYear,
                        onValueChanged = onFromYearChanged,
                        modifier = Modifier.weight(1f),
                    )
                    YearField(
                        label = stringResource(R.string.search_mobile_filter_year_to),
                        value = draftFilters.toYear,
                        onValueChanged = onToYearChanged,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }

        item {
            FilterSection(title = stringResource(R.string.search_mobile_filter_type)) {
                ChipFlow {
                    filterOptions.types.forEach { type ->
                        FilterChip(
                            label = type.title,
                            selected = type.id in draftFilters.types,
                            onClick = { onTypeToggled(type.id) },
                        )
                    }
                }
            }
        }

        item {
            FilterSection(title = stringResource(R.string.search_mobile_filter_status)) {
                ChipFlow {
                    statusOptions().forEach { option ->
                        FilterChip(
                            label = option.label,
                            selected = option.value in draftFilters.statuses,
                            onClick = { onStatusToggled(option.value) },
                        )
                    }
                }
            }
        }

        item {
            FilterSection(title = stringResource(R.string.search_mobile_filter_season)) {
                ChipFlow {
                    seasonOptions().forEach { option ->
                        FilterChip(
                            label = option.label,
                            selected = option.value in draftFilters.seasons,
                            onClick = { onSeasonToggled(option.value) },
                        )
                    }
                }
            }
        }

        item {
            FilterSection(title = stringResource(R.string.search_mobile_filter_age)) {
                ChipFlow {
                    ageOptions().forEach { option ->
                        FilterChip(
                            label = option.label,
                            selected = option.value in draftFilters.ageRatings,
                            onClick = { onAgeRatingToggled(option.value) },
                        )
                    }
                }
            }
        }

        item {
            FilterSection(title = stringResource(R.string.search_mobile_filter_genre_screen_title)) {
                ChipFlow {
                    FilterChip(
                        label = stringResource(
                            R.string.search_mobile_filter_open_genres,
                            draftFilters.genres.size,
                        ),
                        selected = draftFilters.genres.isNotEmpty(),
                        onClick = onOpenGenres,
                    )
                    FilterChip(
                        label = stringResource(
                            R.string.search_mobile_filter_open_exclude_genres,
                            draftFilters.excludedGenres.size,
                        ),
                        selected = draftFilters.excludedGenres.isNotEmpty(),
                        onClick = onOpenExcludedGenres,
                    )
                }
            }
        }
    }
}

@Composable
private fun GenrePickerContent(
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
                    GenreGroup(
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
                GenreGroup(
                    title = stringResource(R.string.search_mobile_filter_genre_screen_title),
                    genres = ungroupedGenres,
                    selectedIds = selectedIds,
                    onGenreToggled = onGenreToggled,
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun GenreGroup(
    title: String,
    genres: List<SearchGenre>,
    selectedIds: Set<String>,
    onGenreToggled: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        ChipFlow {
            genres.forEach { genre ->
                FilterChip(
                    label = genre.title,
                    selected = genre.id in selectedIds,
                    onClick = { onGenreToggled(genre.id) },
                )
            }
        }
    }
}

@Composable
private fun SheetHeader(
    title: String,
    actionLabel: String? = null,
    onClose: (() -> Unit)? = null,
) {
    Box(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .padding(horizontal = 80.dp),
        )
        if (actionLabel != null && onClose != null) {
            TextButton(
                onClick = onClose,
                modifier = Modifier.align(Alignment.CenterEnd),
            ) {
                Text(actionLabel)
            }
        }
    }
}

@Composable
private fun FilterSection(
    title: String,
    content: @Composable () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground,
        )
        content()
        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChipFlow(content: @Composable () -> Unit) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        content()
    }
}

@Composable
private fun YearField(
    label: String,
    value: Int?,
    onValueChanged: (Int?) -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = value?.toString().orEmpty(),
        onValueChange = { text ->
            onValueChanged(text.filter { it.isDigit() }.take(4).toIntOrNull())
        },
        label = { Text(label) },
        singleLine = true,
        shape = RoundedCornerShape(10.dp),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        modifier = modifier,
    )
}

@Composable
private fun FilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(10.dp)
    val borderColor = if (selected) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)
    } else {
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f)
    }
    val backgroundColor = if (selected) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.38f)
    }

    Row(
        modifier = modifier
            .clip(shape)
            .border(width = 1.dp, color = borderColor, shape = shape)
            .background(backgroundColor, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = if (selected) stringResource(
                R.string.search_mobile_filter_selected,
                label
            ) else label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground,
        )
    }
}
