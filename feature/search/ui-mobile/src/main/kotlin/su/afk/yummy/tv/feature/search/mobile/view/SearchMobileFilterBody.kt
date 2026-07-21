package su.afk.yummy.tv.feature.search.mobile.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Sell
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.domain.search.model.SearchFilterOptions
import su.afk.yummy.tv.domain.search.model.SearchFilters
import su.afk.yummy.tv.domain.search.model.SearchSort
import su.afk.yummy.tv.feature.search.mobile.R
import su.afk.yummy.tv.feature.search.mobile.utils.ageOptions
import su.afk.yummy.tv.feature.search.mobile.utils.label
import su.afk.yummy.tv.feature.search.mobile.utils.seasonOptions
import su.afk.yummy.tv.feature.search.mobile.utils.statusOptions

@Composable
internal fun ColumnScope.SearchMobileFilterBody(
    draftFilters: SearchFilters,
    filterOptions: SearchFilterOptions,
    isLoadingFilterOptions: Boolean,
    onBack: () -> Unit,
    onReset: () -> Unit,
    onApply: () -> Unit,
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
        onBack = onBack,
        actionLabel = stringResource(R.string.search_mobile_filters_reset),
        actionVisible = draftFilters.activeCount > 0,
        onClose = onReset,
    )

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
            .weight(1f),
        contentPadding = PaddingValues(bottom = 4.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            FilterSection(title = stringResource(R.string.search_mobile_filter_genre_screen_title)) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterNavigationRow(
                        title = stringResource(R.string.search_mobile_filter_genres),
                        icon = Icons.Filled.Sell,
                        selectedCount = draftFilters.genres.size,
                        onClick = onOpenGenres,
                    )
                    FilterNavigationRow(
                        title = stringResource(R.string.search_mobile_filter_exclude_genres),
                        icon = Icons.Filled.Block,
                        selectedCount = draftFilters.excludedGenres.size,
                        onClick = onOpenExcludedGenres,
                    )
                }
            }
        }

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
                    FilterDirectionChip(
                        label = if (draftFilters.sortForward) {
                            stringResource(R.string.search_mobile_filter_sort_forward)
                        } else {
                            stringResource(R.string.search_mobile_filter_sort_backward)
                        },
                        forward = draftFilters.sortForward,
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
    }

    Button(
        onClick = onApply,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            if (draftFilters.activeCount > 0) {
                stringResource(
                    R.string.search_mobile_filters_apply_with_count,
                    draftFilters.activeCount,
                )
            } else {
                stringResource(R.string.search_mobile_filters_apply)
            },
        )
    }
}
