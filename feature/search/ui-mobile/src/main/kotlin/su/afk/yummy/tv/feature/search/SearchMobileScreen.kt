package su.afk.yummy.tv.feature.search

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import kotlinx.coroutines.flow.Flow
import su.afk.yummy.tv.core.designsystem.presenter.baseScreen.BaseScreen
import su.afk.yummy.tv.core.designsystem.presenter.mobile.MobileMessage
import su.afk.yummy.tv.core.designsystem.presenter.mobile.MobilePosterCard
import su.afk.yummy.tv.core.designsystem.presenter.mobile.MobilePosterGrid
import su.afk.yummy.tv.core.designsystem.presenter.mobile.MobileTopBar
import su.afk.yummy.tv.core.model.ErrorItem
import su.afk.yummy.tv.feature.search.mobile.R
import su.afk.yummy.tv.feature.search.view.RandomAnimeFloatingButton
import su.afk.yummy.tv.feature.search.view.SearchMobileFilterButton
import su.afk.yummy.tv.feature.search.view.SearchMobileFilterSheet

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun SearchMobileScreen(
    state: SearchState.State,
    effect: Flow<SearchState.Effect>,
    onEvent: (SearchState.Event) -> Unit,
) {
    val results = state.results.collectAsLazyPagingItems()
    val refreshState = results.loadState.refresh
    val appendState = results.loadState.append
    val hasActiveSearch = state.isSearchActive
    val initialError = (refreshState as? LoadState.Error)
        ?.takeIf { results.itemCount == 0 }
        ?.error
        ?.uiMessage()
    val gridState = rememberLazyGridState()
    val keyboardController = LocalSoftwareKeyboardController.current
    val queryFocusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        queryFocusRequester.requestFocus()
    }

    BaseScreen(
        isScroll = false,
        topBar = {
            MobileTopBar(
                title = stringResource(R.string.search_mobile_title),
                onBack = { onEvent(SearchState.Event.BackSelected) },
            )
        },
        isLoading = hasActiveSearch && refreshState is LoadState.Loading && results.itemCount == 0,
        error = initialError?.let { ErrorItem(title = it, message = it) },
        onRetry = {
            onEvent(SearchState.Event.RetrySelected)
            results.retry()
        },
        errorContent = initialError?.let { message ->
            { _, retry ->
                MobileMessage(
                    title = message,
                    actionLabel = stringResource(R.string.search_mobile_submit),
                    onAction = retry,
                )
            }
        },
        floatingActionButtonEnd = {
            RandomAnimeFloatingButton(
                isLoading = state.isRandomAnimeLoading,
                onClick = { onEvent(SearchState.Event.RandomAnimeSelected) },
            )
        },
        floatingActionButtonBottomPadding = 8.dp,
    ) {
        MobilePosterGrid(
            contentPadding = PaddingValues(0.dp),
            state = gridState,
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                OutlinedTextField(
                    value = state.query,
                    onValueChange = { onEvent(SearchState.Event.QueryChanged(it)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(queryFocusRequester),
                    label = { Text(stringResource(R.string.search_mobile_query)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = {
                        keyboardController?.hide()
                        onEvent(SearchState.Event.SearchSubmitted)
                    }),
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
            if (hasActiveSearch && refreshState !is LoadState.Loading) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Text(
                        text = stringResource(
                            R.string.search_mobile_results_count,
                            results.itemCount
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            val error = (appendState as? LoadState.Error)?.error?.uiMessage()
            if (error != null && results.itemCount > 0) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Text(error, color = MaterialTheme.colorScheme.error)
                }
            }
            items(
                count = results.itemCount,
                key = results.itemKey { it.id },
            ) { index ->
                results[index]?.let { item ->
                    MobilePosterCard(
                        title = item.title,
                        posterUrl = item.posterUrl,
                        rating = item.rating,
                        posterOverlay = {
                            item.year?.let { year ->
                                Text(
                                    text = year.toString(),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.inverseSurface,
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .padding(4.dp)
                                        .background(
                                            MaterialTheme.colorScheme.inverseOnSurface,
                                            RoundedCornerShape(4.dp),
                                        )
                                        .padding(horizontal = 6.dp, vertical = 3.dp),
                                )
                            }
                        },
                        onClick = { onEvent(SearchState.Event.ItemSelected(item.id)) },
                    )
                }
            }
            if (appendState is LoadState.Loading && results.itemCount > 0) {
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

private fun Throwable.uiMessage(): String =
    message ?: localizedMessage ?: toString()
