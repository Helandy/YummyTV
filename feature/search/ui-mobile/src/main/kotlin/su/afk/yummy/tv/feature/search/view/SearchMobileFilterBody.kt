package su.afk.yummy.tv.feature.search.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.domain.search.model.SearchFilterOptions
import su.afk.yummy.tv.domain.search.model.SearchFilters
import su.afk.yummy.tv.domain.search.model.SearchSort
import su.afk.yummy.tv.feature.search.mobile.R
import su.afk.yummy.tv.feature.search.utils.ageOptions
import su.afk.yummy.tv.feature.search.utils.label
import su.afk.yummy.tv.feature.search.utils.seasonOptions
import su.afk.yummy.tv.feature.search.utils.statusOptions

@Composable
internal fun SearchMobileFilterBody(
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
    SheetHeader(title = stringResource(R.string.search_mobile_filters))

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
