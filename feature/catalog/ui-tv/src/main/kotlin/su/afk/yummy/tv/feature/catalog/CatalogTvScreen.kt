package su.afk.yummy.tv.feature.catalog

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.tooling.preview.Preview
import androidx.paging.compose.collectAsLazyPagingItems
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import su.afk.yummy.tv.core.designsystem.preview.ScreenPreviewTheme
import su.afk.yummy.tv.domain.catalog.model.SearchItem
import su.afk.yummy.tv.domain.catalog.model.SearchSort
import su.afk.yummy.tv.feature.catalog.navigator.CatalogDestination
import su.afk.yummy.tv.feature.catalog.view.CatalogResultsPane

@Preview(
    name = "Default",
    device = "spec:width=1920dp,height=1080dp,dpi=160",
    uiMode = android.content.res.Configuration.UI_MODE_TYPE_TELEVISION,
    showBackground = true
)
@Composable
private fun CatalogTvScreenDefaultPreview() = ScreenPreviewTheme {
    CatalogTvScreen(CatalogDestination(), CatalogState.State(), emptyFlow()) {}
}

@Composable
fun CatalogTvScreen(
    dest: CatalogDestination,
    state: CatalogState.State,
    effect: Flow<CatalogState.Effect>,
    onEvent: (CatalogState.Event) -> Unit,
) {
    LaunchedEffect(dest.initialQuery) {
        if (dest.initialQuery.isNotBlank()) {
            onEvent(CatalogState.Event.ExternalSearchSubmitted(dest.initialQuery))
        }
    }

    val results = state.results.collectAsLazyPagingItems()
    val onQueryChanged =
        remember(onEvent) { { q: String -> onEvent(CatalogState.Event.QueryChanged(q)) } }
    val onSearchSubmitted = remember(onEvent) { { onEvent(CatalogState.Event.SearchSubmitted) } }
    val onRandomAnimeSelected =
        remember(onEvent) { { onEvent(CatalogState.Event.RandomAnimeSelected) } }
    val onRetry = remember(onEvent, results) {
        {
            onEvent(CatalogState.Event.RetrySelected)
            results.retry()
        }
    }
    val onItemSelected =
        remember(onEvent) { { item: SearchItem -> onEvent(CatalogState.Event.ItemSelected(item.id)) } }
    val onOpenFilters = remember(onEvent) { { onEvent(CatalogState.Event.OpenFilters) } }
    val onCloseFilters = remember(onEvent) { { onEvent(CatalogState.Event.CloseFilters) } }
    val onApplyFilters = remember(onEvent) { { onEvent(CatalogState.Event.ApplyFilters) } }
    val onResetFilters = remember(onEvent) { { onEvent(CatalogState.Event.ResetFilters) } }
    val onGenreToggled =
        remember(onEvent) { { id: String -> onEvent(CatalogState.Event.GenreToggled(id)) } }
    val onExcludedGenreToggled =
        remember(onEvent) { { id: String -> onEvent(CatalogState.Event.ExcludedGenreToggled(id)) } }
    val onTypeToggled =
        remember(onEvent) { { id: String -> onEvent(CatalogState.Event.TypeToggled(id)) } }
    val onStatusToggled =
        remember(onEvent) { { id: String -> onEvent(CatalogState.Event.StatusToggled(id)) } }
    val onSeasonToggled =
        remember(onEvent) { { id: String -> onEvent(CatalogState.Event.SeasonToggled(id)) } }
    val onAgeRatingToggled =
        remember(onEvent) { { value: Int -> onEvent(CatalogState.Event.AgeRatingToggled(value)) } }
    val onFromYearChanged =
        remember(onEvent) { { year: Int? -> onEvent(CatalogState.Event.FromYearChanged(year)) } }
    val onToYearChanged =
        remember(onEvent) { { year: Int? -> onEvent(CatalogState.Event.ToYearChanged(year)) } }
    val onSortSelected =
        remember(onEvent) { { sort: SearchSort -> onEvent(CatalogState.Event.SortSelected(sort)) } }
    val onSortDirectionToggled =
        remember(onEvent) { { onEvent(CatalogState.Event.SortDirectionToggled) } }

    CatalogResultsPane(
        query = state.query,
        results = results,
        filters = state.filters,
        isSearchActive = state.isSearchActive,
        showTitleYear = state.showTitleYear,
        isSearchPending = state.isSearchPending,
        draftFilters = state.draftFilters,
        filterOptions = state.filterOptions,
        isFilterPanelOpen = state.isFilterPanelOpen,
        isLoadingFilterOptions = state.isLoadingFilterOptions,
        isRandomAnimeLoading = state.isRandomAnimeLoading,
        onQueryChanged = onQueryChanged,
        onSearchSubmitted = onSearchSubmitted,
        onRandomAnimeSelected = onRandomAnimeSelected,
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
