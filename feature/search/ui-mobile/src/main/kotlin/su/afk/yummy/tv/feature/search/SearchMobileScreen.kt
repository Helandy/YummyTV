package su.afk.yummy.tv.feature.search

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.Flow
import su.afk.yummy.tv.core.designsystem.presenter.baseScreen.BaseScreen
import su.afk.yummy.tv.core.designsystem.presenter.mobile.MobileMessage
import su.afk.yummy.tv.core.designsystem.presenter.mobile.MobilePosterCard
import su.afk.yummy.tv.core.designsystem.presenter.mobile.MobilePosterGrid
import su.afk.yummy.tv.core.model.ErrorItem
import su.afk.yummy.tv.feature.search.mobile.R
import su.afk.yummy.tv.feature.search.view.SearchMobileFilterButton
import su.afk.yummy.tv.feature.search.view.SearchMobileFilterSheet

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun SearchMobileScreen(
    state: SearchState.State,
    effect: Flow<SearchState.Effect>,
    onEvent: (SearchState.Event) -> Unit,
) {
    val initialError = state.error?.takeIf { state.items.isEmpty() }
    BaseScreen(
        isScroll = false,
        topBar = { Text(stringResource(R.string.search_mobile_title)) },
        isLoading = state.isLoading && state.items.isEmpty(),
        error = initialError?.let { ErrorItem(title = it, message = it) },
        onRetry = { onEvent(SearchState.Event.SearchSubmitted) },
        errorContent = initialError?.let { message ->
            { _, retry ->
                MobileMessage(
                    title = message,
                    actionLabel = stringResource(R.string.search_mobile_submit),
                    onAction = retry,
                )
            }
        },
    ) {
        MobilePosterGrid(contentPadding = PaddingValues(0.dp)) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                OutlinedTextField(
                    value = state.query,
                    onValueChange = { onEvent(SearchState.Event.QueryChanged(it)) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.search_mobile_query)) },
                    singleLine = true,
                    trailingIcon = {
                        SearchMobileFilterButton(
                            activeCount = state.filters.activeCount,
                            onClick = { onEvent(SearchState.Event.OpenFilters) },
                        )
                    },
                )
            }
            item(span = { GridItemSpan(maxLineSpan) }) {
                Button(
                    onClick = { onEvent(SearchState.Event.SearchSubmitted) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        if (state.filters.activeCount > 0) {
                            stringResource(
                                R.string.search_mobile_submit_with_filters,
                                state.filters.activeCount
                            )
                        } else {
                            stringResource(R.string.search_mobile_submit)
                        },
                    )
                }
            }
            val error = state.error
            if (error != null && state.items.isNotEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Text(error, color = MaterialTheme.colorScheme.error)
                }
            }
            items(state.items, key = { it.id }) { item ->
                MobilePosterCard(
                    title = item.title,
                    posterUrl = item.posterUrl,
                    rating = item.rating,
                    onClick = { onEvent(SearchState.Event.ItemSelected(item.id)) },
                )
            }
            if (state.canLoadMore) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Button(
                        onClick = { onEvent(SearchState.Event.LoadMore) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            if (state.isLoading) {
                                stringResource(R.string.search_mobile_loading)
                            } else {
                                stringResource(R.string.search_mobile_load_more)
                            },
                        )
                    }
                }
            }
        }
    }

    if (state.isFilterPanelOpen) {
        SearchMobileFilterSheet(
            draftFilters = state.draftFilters,
            filterOptions = state.filterOptions,
            isLoadingFilterOptions = state.isLoadingFilterOptions,
            onClose = { onEvent(SearchState.Event.CloseFilters) },
            onApply = { onEvent(SearchState.Event.ApplyFilters) },
            onReset = { onEvent(SearchState.Event.ResetFilters) },
            onGenreToggled = { onEvent(SearchState.Event.GenreToggled(it)) },
            onExcludedGenreToggled = { onEvent(SearchState.Event.ExcludedGenreToggled(it)) },
            onTypeToggled = { onEvent(SearchState.Event.TypeToggled(it)) },
            onStatusToggled = { onEvent(SearchState.Event.StatusToggled(it)) },
            onSeasonToggled = { onEvent(SearchState.Event.SeasonToggled(it)) },
            onAgeRatingToggled = { onEvent(SearchState.Event.AgeRatingToggled(it)) },
            onFromYearChanged = { onEvent(SearchState.Event.FromYearChanged(it)) },
            onToYearChanged = { onEvent(SearchState.Event.ToYearChanged(it)) },
            onSortSelected = { onEvent(SearchState.Event.SortSelected(it)) },
            onSortDirectionToggled = { onEvent(SearchState.Event.SortDirectionToggled) },
        )
    }
}
