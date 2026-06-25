package su.afk.yummy.tv.feature.search

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.paging.compose.collectAsLazyPagingItems
import kotlinx.coroutines.flow.Flow
import su.afk.yummy.tv.domain.search.model.SearchItem
import su.afk.yummy.tv.domain.search.model.SearchSort
import su.afk.yummy.tv.feature.search.view.SearchResultsPane

@Composable
fun SearchTvScreen(
    state: SearchState.State,
    effect: Flow<SearchState.Effect>,
    onEvent: (SearchState.Event) -> Unit,
) {
    val results = state.results.collectAsLazyPagingItems()
    val onQueryChanged = remember(onEvent) { { q: String -> onEvent(SearchState.Event.QueryChanged(q)) } }
    val onSearchSubmitted = remember(onEvent) { { onEvent(SearchState.Event.SearchSubmitted) } }
    val onRetry = remember(onEvent, results) {
        {
            onEvent(SearchState.Event.RetrySelected)
            results.retry()
        }
    }
    val onItemSelected = remember(onEvent) { { item: SearchItem -> onEvent(SearchState.Event.ItemSelected(item.id)) } }
    val onOpenFilters = remember(onEvent) { { onEvent(SearchState.Event.OpenFilters) } }
    val onCloseFilters = remember(onEvent) { { onEvent(SearchState.Event.CloseFilters) } }
    val onApplyFilters = remember(onEvent) { { onEvent(SearchState.Event.ApplyFilters) } }
    val onResetFilters = remember(onEvent) { { onEvent(SearchState.Event.ResetFilters) } }
    val onGenreToggled = remember(onEvent) { { id: String -> onEvent(SearchState.Event.GenreToggled(id)) } }
    val onExcludedGenreToggled = remember(onEvent) { { id: String -> onEvent(SearchState.Event.ExcludedGenreToggled(id)) } }
    val onTypeToggled = remember(onEvent) { { id: String -> onEvent(SearchState.Event.TypeToggled(id)) } }
    val onStatusToggled = remember(onEvent) { { id: String -> onEvent(SearchState.Event.StatusToggled(id)) } }
    val onSeasonToggled = remember(onEvent) { { id: String -> onEvent(SearchState.Event.SeasonToggled(id)) } }
    val onAgeRatingToggled = remember(onEvent) { { value: Int -> onEvent(SearchState.Event.AgeRatingToggled(value)) } }
    val onFromYearChanged = remember(onEvent) { { year: Int? -> onEvent(SearchState.Event.FromYearChanged(year)) } }
    val onToYearChanged = remember(onEvent) { { year: Int? -> onEvent(SearchState.Event.ToYearChanged(year)) } }
    val onSortSelected = remember(onEvent) { { sort: SearchSort -> onEvent(SearchState.Event.SortSelected(sort)) } }
    val onSortDirectionToggled = remember(onEvent) { { onEvent(SearchState.Event.SortDirectionToggled) } }

    SearchResultsPane(
        query = state.query,
        results = results,
        filters = state.filters,
        isSearchActive = state.isSearchActive,
        draftFilters = state.draftFilters,
        filterOptions = state.filterOptions,
        isFilterPanelOpen = state.isFilterPanelOpen,
        isLoadingFilterOptions = state.isLoadingFilterOptions,
        onQueryChanged = onQueryChanged,
        onSearchSubmitted = onSearchSubmitted,
        onRetry = onRetry,
        onItemSelected = onItemSelected,
        onOpenFilters = onOpenFilters,
        onCloseFilters = onCloseFilters,
        onApplyFilters = onApplyFilters,
        onResetFilters = onResetFilters,
        onGenreToggled = onGenreToggled,
        onExcludedGenreToggled = onExcludedGenreToggled,
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
