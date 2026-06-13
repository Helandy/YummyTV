package su.afk.yummy.tv.feature.search.view

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.core.designsystem.presenter.dimensions.TvScreenPadding
import su.afk.yummy.tv.domain.search.model.SearchFilterOptions
import su.afk.yummy.tv.domain.search.model.SearchFilters
import su.afk.yummy.tv.domain.search.model.SearchSort
import su.afk.yummy.tv.feature.search.R
import su.afk.yummy.tv.feature.search.model.GenrePickerMode
import su.afk.yummy.tv.feature.search.utils.ageOptions
import su.afk.yummy.tv.feature.search.utils.label
import su.afk.yummy.tv.feature.search.utils.seasonOptions
import su.afk.yummy.tv.feature.search.utils.statusOptions

@Composable
internal fun FilterPanel(
    draftFilters: SearchFilters,
    filterOptions: SearchFilterOptions,
    isLoadingFilterOptions: Boolean,
    initialFocusRequester: FocusRequester,
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
    modifier: Modifier = Modifier,
) {
    var genrePickerMode by remember { mutableStateOf<GenrePickerMode?>(null) }
    var restoreGenreButtonMode by remember { mutableStateOf<GenrePickerMode?>(null) }
    val currentGenrePickerMode = genrePickerMode
    val includeGenresFocusRequester = remember { FocusRequester() }
    val excludeGenresFocusRequester = remember { FocusRequester() }
    val genreBackFocusRequester = remember { FocusRequester() }
    val resetFocusRequester = remember { FocusRequester() }
    val closeFocusRequester = remember { FocusRequester() }
    val firstSortFocusRequester = remember { FocusRequester() }
    val sortDirectionFocusRequester = remember { FocusRequester() }
    val fromYearFocusRequester = remember { FocusRequester() }
    val toYearFocusRequester = remember { FocusRequester() }
    val firstTypeFocusRequester = remember { FocusRequester() }
    val firstStatusFocusRequester = remember { FocusRequester() }
    val firstSeasonFocusRequester = remember { FocusRequester() }
    val firstAgeFocusRequester = remember { FocusRequester() }
    val yearDownFocusRequester = if (filterOptions.types.isNotEmpty()) {
        firstTypeFocusRequester
    } else {
        firstStatusFocusRequester
    }

    LaunchedEffect(Unit) {
        repeat(6) {
            runCatching { initialFocusRequester.requestFocus() }
            withFrameNanos { }
        }
    }

    LaunchedEffect(currentGenrePickerMode, restoreGenreButtonMode) {
        val pickerMode = currentGenrePickerMode
        if (pickerMode != null) {
            runCatching { genreBackFocusRequester.requestFocus() }
            return@LaunchedEffect
        }

        when (restoreGenreButtonMode) {
            GenrePickerMode.INCLUDE -> runCatching { includeGenresFocusRequester.requestFocus() }
            GenrePickerMode.EXCLUDE -> runCatching { excludeGenresFocusRequester.requestFocus() }
            null -> Unit
        }
        restoreGenreButtonMode = null
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .widthIn(max = 920.dp)
            .padding(horizontal = TvScreenPadding.Horizontal, vertical = 8.dp)
            .clip(RoundedCornerShape(18.dp))
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                shape = RoundedCornerShape(18.dp),
            )
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.62f))
            .padding(horizontal = 24.dp, vertical = 20.dp)
            .verticalScroll(rememberScrollState())
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown || event.key != Key.Back) {
                    return@onPreviewKeyEvent false
                }
                if (currentGenrePickerMode != null) {
                    restoreGenreButtonMode = currentGenrePickerMode
                    genrePickerMode = null
                } else {
                    onClose()
                }
                true
            }
            .focusGroup(),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        if (currentGenrePickerMode != null) {
            GenrePickerScreen(
                title = when (currentGenrePickerMode) {
                    GenrePickerMode.INCLUDE -> stringResource(R.string.search_filter_genres)
                    GenrePickerMode.EXCLUDE -> stringResource(R.string.search_filter_exclude_genres)
                },
                selectedIds = when (currentGenrePickerMode) {
                    GenrePickerMode.INCLUDE -> draftFilters.genres
                    GenrePickerMode.EXCLUDE -> draftFilters.excludedGenres
                },
                filterOptions = filterOptions,
                onGenreToggled = when (currentGenrePickerMode) {
                    GenrePickerMode.INCLUDE -> onGenreToggled
                    GenrePickerMode.EXCLUDE -> onExcludedGenreToggled
                },
                backFocusRequester = genreBackFocusRequester,
                onBack = {
                    restoreGenreButtonMode = currentGenrePickerMode
                    genrePickerMode = null
                },
            )
            return@Column
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.search_filters),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                SelectableRow(
                    label = stringResource(R.string.search_filters_apply),
                    selected = true,
                    onClick = onApply,
                    modifier = Modifier
                        .focusRequester(initialFocusRequester)
                        .focusProperties {
                            up = FocusRequester.Cancel
                            left = FocusRequester.Cancel
                            right = resetFocusRequester
                            down = firstSortFocusRequester
                        },
                )
                SelectableRow(
                    label = stringResource(R.string.search_filters_reset),
                    selected = false,
                    onClick = onReset,
                    modifier = Modifier
                        .focusRequester(resetFocusRequester)
                        .focusProperties {
                            up = FocusRequester.Cancel
                            left = initialFocusRequester
                            right = closeFocusRequester
                            down = firstSortFocusRequester
                        },
                )
                SelectableRow(
                    label = stringResource(R.string.search_filters_close),
                    selected = false,
                    onClick = onClose,
                    modifier = Modifier
                        .focusRequester(closeFocusRequester)
                        .focusProperties {
                            up = FocusRequester.Cancel
                            left = resetFocusRequester
                            right = FocusRequester.Cancel
                            down = firstSortFocusRequester
                        },
                )
            }
        }

        if (isLoadingFilterOptions) {
            Text(
                text = stringResource(R.string.search_filters_loading),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        FilterSection(title = stringResource(R.string.search_filter_sort)) {
            ChipFlow {
                SearchSort.entries.forEachIndexed { index, sort ->
                    SelectableRow(
                        label = sort.label(),
                        selected = draftFilters.sort == sort,
                        onClick = { onSortSelected(sort) },
                        modifier = Modifier
                            .then(
                                if (index == 0) {
                                    Modifier.focusRequester(firstSortFocusRequester)
                                } else {
                                    Modifier
                                },
                            )
                            .focusProperties {
                                up = initialFocusRequester
                                down = sortDirectionFocusRequester
                            },
                    )
                }
                SelectableRow(
                    label = if (draftFilters.sortForward) {
                        stringResource(R.string.search_filter_sort_forward)
                    } else {
                        stringResource(R.string.search_filter_sort_backward)
                    },
                    selected = !draftFilters.sortForward,
                    onClick = onSortDirectionToggled,
                    modifier = Modifier
                        .focusRequester(sortDirectionFocusRequester)
                        .focusProperties {
                            up = firstSortFocusRequester
                            down = fromYearFocusRequester
                        },
                )
            }
        }

        FilterSection(title = stringResource(R.string.search_filter_year)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                YearField(
                    label = stringResource(R.string.search_filter_year_from),
                    value = draftFilters.fromYear,
                    onValueChanged = onFromYearChanged,
                    focusRequester = fromYearFocusRequester,
                    upFocusRequester = sortDirectionFocusRequester,
                    downFocusRequester = yearDownFocusRequester,
                    rightFocusRequester = toYearFocusRequester,
                    modifier = Modifier.weight(1f),
                )
                YearField(
                    label = stringResource(R.string.search_filter_year_to),
                    value = draftFilters.toYear,
                    onValueChanged = onToYearChanged,
                    focusRequester = toYearFocusRequester,
                    upFocusRequester = sortDirectionFocusRequester,
                    downFocusRequester = yearDownFocusRequester,
                    leftFocusRequester = fromYearFocusRequester,
                    modifier = Modifier.weight(1f),
                )
            }
        }

        FilterSection(title = stringResource(R.string.search_filter_type)) {
            ChipFlow {
                filterOptions.types.forEachIndexed { index, type ->
                    SelectableRow(
                        label = type.title,
                        selected = type.id in draftFilters.types,
                        onClick = { onTypeToggled(type.id) },
                        modifier = Modifier
                            .then(
                                if (index == 0) {
                                    Modifier.focusRequester(firstTypeFocusRequester)
                                } else {
                                    Modifier
                                },
                            )
                            .focusProperties {
                                up = fromYearFocusRequester
                                down = firstStatusFocusRequester
                            },
                    )
                }
            }
        }

        FilterSection(title = stringResource(R.string.search_filter_status)) {
            ChipFlow {
                statusOptions().forEachIndexed { index, option ->
                    SelectableRow(
                        label = option.label,
                        selected = option.value in draftFilters.statuses,
                        onClick = { onStatusToggled(option.value) },
                        modifier = Modifier
                            .then(
                                if (index == 0) {
                                    Modifier.focusRequester(firstStatusFocusRequester)
                                } else {
                                    Modifier
                                },
                            )
                            .focusProperties {
                                up = yearDownFocusRequester
                                down = firstSeasonFocusRequester
                            },
                    )
                }
            }
        }

        FilterSection(title = stringResource(R.string.search_filter_season)) {
            ChipFlow {
                seasonOptions().forEachIndexed { index, option ->
                    SelectableRow(
                        label = option.label,
                        selected = option.value in draftFilters.seasons,
                        onClick = { onSeasonToggled(option.value) },
                        modifier = Modifier
                            .then(
                                if (index == 0) {
                                    Modifier.focusRequester(firstSeasonFocusRequester)
                                } else {
                                    Modifier
                                },
                            )
                            .focusProperties {
                                up = firstStatusFocusRequester
                                down = firstAgeFocusRequester
                            },
                    )
                }
            }
        }

        FilterSection(title = stringResource(R.string.search_filter_age)) {
            ChipFlow {
                ageOptions().forEachIndexed { index, option ->
                    SelectableRow(
                        label = option.label,
                        selected = option.value in draftFilters.ageRatings,
                        onClick = { onAgeRatingToggled(option.value) },
                        modifier = Modifier
                            .then(
                                if (index == 0) {
                                    Modifier.focusRequester(firstAgeFocusRequester)
                                } else {
                                    Modifier
                                },
                            )
                            .focusProperties {
                                up = firstSeasonFocusRequester
                                down = includeGenresFocusRequester
                            },
                    )
                }
            }
        }

        FilterSection(title = stringResource(R.string.search_filter_genre_screen_title)) {
            ChipFlow {
                SelectableRow(
                    label = stringResource(
                        R.string.search_filter_open_genres,
                        draftFilters.genres.size
                    ),
                    selected = draftFilters.genres.isNotEmpty(),
                    onClick = { genrePickerMode = GenrePickerMode.INCLUDE },
                    modifier = Modifier
                        .focusRequester(includeGenresFocusRequester)
                        .focusProperties {
                            up = firstAgeFocusRequester
                            right = excludeGenresFocusRequester
                        },
                )
                SelectableRow(
                    label = stringResource(
                        R.string.search_filter_open_exclude_genres,
                        draftFilters.excludedGenres.size,
                    ),
                    selected = draftFilters.excludedGenres.isNotEmpty(),
                    onClick = { genrePickerMode = GenrePickerMode.EXCLUDE },
                    modifier = Modifier
                        .focusRequester(excludeGenresFocusRequester)
                        .focusProperties {
                            up = firstAgeFocusRequester
                            left = includeGenresFocusRequester
                        },
                )
            }
        }
    }
}
