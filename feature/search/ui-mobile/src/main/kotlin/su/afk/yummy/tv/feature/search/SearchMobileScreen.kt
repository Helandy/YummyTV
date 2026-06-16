package su.afk.yummy.tv.feature.search

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.Flow
import su.afk.yummy.tv.core.designsystem.presenter.baseScreen.BaseScreen
import su.afk.yummy.tv.core.designsystem.presenter.mobile.MobileMessage
import su.afk.yummy.tv.core.designsystem.presenter.mobile.MobilePosterCard
import su.afk.yummy.tv.core.designsystem.presenter.mobile.MobilePosterGrid
import su.afk.yummy.tv.core.designsystem.presenter.mobile.MobileTopBar
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
    val gridState = rememberLazyGridState()
    val shouldLoadMore by remember(
        gridState,
        state.items.size,
        state.canLoadMore,
        state.isLoading
    ) {
        derivedStateOf {
            val lastVisible = gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            val total = gridState.layoutInfo.totalItemsCount
            state.items.isNotEmpty() &&
                    state.canLoadMore &&
                    !state.isLoading &&
                    total > 0 &&
                    lastVisible >= total - LOAD_MORE_THRESHOLD
        }
    }

    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) {
            onEvent(SearchState.Event.LoadMore)
        }
    }

    BaseScreen(
        isScroll = false,
        topBar = {
            MobileTopBar(
                title = stringResource(R.string.search_mobile_title),
                onBack = { onEvent(SearchState.Event.BackSelected) },
            )
        },
        isLoading = state.isLoading && state.items.isEmpty(),
        error = initialError?.let { ErrorItem(title = it, message = it) },
        onRetry = { onEvent(SearchState.Event.RetrySelected) },
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
        MobilePosterGrid(
            contentPadding = PaddingValues(0.dp),
            state = gridState,
        ) {
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
            if (state.isLoading && state.items.isNotEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
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
            onClose = { onEvent(SearchState.Event.ApplyFilters) },
            onReset = { onEvent(SearchState.Event.ResetDraftFilters) },
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

private const val LOAD_MORE_THRESHOLD = 6
