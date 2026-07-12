package su.afk.yummy.tv.feature.search.view

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import kotlinx.coroutines.Job
import su.afk.yummy.tv.core.designsystem.presenter.components.loader.TvLoadingScreen
import su.afk.yummy.tv.core.designsystem.presenter.focus.TvRetryButton
import su.afk.yummy.tv.core.designsystem.presenter.focus.launchTvLazyGridKeyFocusRestore
import su.afk.yummy.tv.core.designsystem.presenter.focus.rememberTvLazyFocusRestoreState
import su.afk.yummy.tv.core.designsystem.presenter.locals.LocalMainMenuFocusRequester
import su.afk.yummy.tv.core.designsystem.presenter.locals.LocalPreferredContentFocusRequester
import su.afk.yummy.tv.domain.search.model.SearchFilterOptions
import su.afk.yummy.tv.domain.search.model.SearchFilters
import su.afk.yummy.tv.domain.search.model.SearchItem
import su.afk.yummy.tv.domain.search.model.SearchSort
import su.afk.yummy.tv.feature.search.R

@Composable
internal fun SearchResultsPane(
    query: String,
    results: LazyPagingItems<SearchItem>,
    filters: SearchFilters,
    isSearchActive: Boolean,
    draftFilters: SearchFilters,
    filterOptions: SearchFilterOptions,
    isFilterPanelOpen: Boolean,
    isLoadingFilterOptions: Boolean,
    isRandomAnimeLoading: Boolean,
    onQueryChanged: (String) -> Unit,
    onSearchSubmitted: () -> Unit,
    onRandomAnimeSelected: () -> Unit,
    onRetry: () -> Unit,
    onItemSelected: (SearchItem) -> Unit,
    onOpenFilters: () -> Unit,
    onCloseFilters: () -> Unit,
    onApplyFilters: () -> Unit,
    onResetFilters: () -> Unit,
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
    val scope = rememberCoroutineScope()
    val gridState = rememberLazyGridState()
    val refreshState = results.loadState.refresh
    val appendState = results.loadState.append
    val itemCount = results.itemCount
    val snapshotItems = results.itemSnapshotList.items
    val hasActiveSearch = isSearchActive
    val isLoading = hasActiveSearch && refreshState is LoadState.Loading
    val itemIds = remember(snapshotItems) { snapshotItems.map { it.id } }
    val focusRequesters = remember(itemCount) { List(itemCount) { FocusRequester() } }
    val gridFocusRequester = remember { FocusRequester() }
    val searchFieldFocusRequester = remember { FocusRequester() }
    val filterButtonFocusRequester = remember { FocusRequester() }
    val randomButtonFocusRequester = remember { FocusRequester() }
    val retryFocusRequester = remember { FocusRequester() }
    val registerPreferredContentFocusRequester = LocalPreferredContentFocusRequester.current
    val mainMenuFocusRequester = LocalMainMenuFocusRequester.current
    val filterPanelInitialFocusRequester = remember { FocusRequester() }
    val focusStateKey = remember(query, filters) { "${query.trim()}|${filters.focusStateKey()}" }
    val focusRestoreState = rememberTvLazyFocusRestoreState<Int>(focusStateKey)
    val itemFocusRequesters = remember(itemIds, focusRequesters) {
        itemIds.zip(focusRequesters).toMap()
    }
    var gridHasFocus by remember { mutableStateOf(false) }
    var isRestoringFocus by remember { mutableStateOf(false) }
    var restoreFilterButtonFocusToken by rememberSaveable { mutableIntStateOf(0) }
    var restoreFocusJob by remember { mutableStateOf<Job?>(null) }

    fun rememberFocusedItem(index: Int) {
        itemIds.getOrNull(index)?.let { key ->
            focusRestoreState.onItemFocused(key, index)
        }
    }

    fun restoreTargetIndex(): Int {
        if (itemCount == 0) return 0
        return focusRestoreState.targetIndex(itemIds)?.coerceIn(0, itemCount - 1) ?: 0
    }

    fun gridFallbackFocusRequester(): FocusRequester =
        focusRequesters.getOrNull(restoreTargetIndex()) ?: searchFieldFocusRequester

    fun requestResultFocus(
        index: Int,
        fallbackFocusRequester: FocusRequester? = null,
    ) {
        if (itemCount == 0) return
        val target = index.coerceIn(0, itemCount - 1)
        rememberFocusedItem(target)
        isRestoringFocus = true
        restoreFocusJob = launchTvLazyGridKeyFocusRestore(
            previousJob = restoreFocusJob,
            scope = scope,
            restoreState = focusRestoreState,
            keys = itemIds,
            gridState = gridState,
            itemFocusRequesters = itemFocusRequesters,
            fallbackFocusRequester = fallbackFocusRequester ?: gridFallbackFocusRequester(),
            fallbackIndex = target,
            onRestoreFinished = {
                isRestoringFocus = false
            },
        )
    }

    val preferredContentFocusRequester = when {
        isFilterPanelOpen -> filterPanelInitialFocusRequester
        restoreFilterButtonFocusToken > 0 -> filterButtonFocusRequester
        refreshState is LoadState.Error && itemCount == 0 && !isLoading -> retryFocusRequester
        itemCount > 0 -> gridFocusRequester

        else -> searchFieldFocusRequester
    }
    val initialError = (refreshState as? LoadState.Error)
        ?.takeIf { itemCount == 0 }
        ?.error
        ?.uiMessage()

    DisposableEffect(preferredContentFocusRequester, registerPreferredContentFocusRequester) {
        registerPreferredContentFocusRequester?.invoke(preferredContentFocusRequester)
        onDispose { registerPreferredContentFocusRequester?.invoke(null) }
    }

    DisposableEffect(Unit) {
        onDispose { restoreFocusJob?.cancel() }
    }

    LaunchedEffect(restoreFilterButtonFocusToken, isFilterPanelOpen) {
        if (restoreFilterButtonFocusToken <= 0 || isFilterPanelOpen) return@LaunchedEffect
        var focused = false
        repeat(18) {
            withFrameNanos { }
            focused = runCatching { filterButtonFocusRequester.requestFocus() }
                .getOrDefault(false) || focused
        }
        if (focused) {
            restoreFilterButtonFocusToken = 0
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        SearchTvHeaderRow(
            query = query,
            filters = filters,
            searchFieldFocusRequester = searchFieldFocusRequester,
            filterButtonFocusRequester = filterButtonFocusRequester,
            randomButtonFocusRequester = randomButtonFocusRequester,
            mainMenuFocusRequester = mainMenuFocusRequester,
            onQueryChanged = onQueryChanged,
            onSearchSubmitted = onSearchSubmitted,
            onOpenFilters = onOpenFilters,
            isRandomAnimeLoading = isRandomAnimeLoading,
            onRandomAnimeSelected = onRandomAnimeSelected,
        )

        Box(modifier = Modifier.fillMaxSize()) {
            when {
                isFilterPanelOpen -> FilterPanel(
                    draftFilters = draftFilters,
                    filterOptions = filterOptions,
                    isLoadingFilterOptions = isLoadingFilterOptions,
                    initialFocusRequester = filterPanelInitialFocusRequester,
                    onClose = {
                        runCatching { filterButtonFocusRequester.requestFocus() }
                        restoreFilterButtonFocusToken += 1
                        onCloseFilters()
                    },
                    onApply = {
                        runCatching { filterButtonFocusRequester.requestFocus() }
                        restoreFilterButtonFocusToken += 1
                        onApplyFilters()
                    },
                    onReset = onResetFilters,
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
                    modifier = Modifier.align(Alignment.TopCenter),
                )

                itemCount == 0 && isLoading -> TvLoadingScreen()
                initialError != null -> SearchErrorMessage(
                    message = initialError,
                    retryFocusRequester = retryFocusRequester,
                    onRetry = onRetry,
                    modifier = Modifier.align(Alignment.Center),
                )

                itemCount == 0 && hasActiveSearch -> {
                    Text(
                        text = stringResource(R.string.search_empty),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.align(Alignment.Center),
                    )
                }

                else -> {
                    SearchResultsGrid(
                        results = results,
                        isLoading = appendState is LoadState.Loading,
                        gridState = gridState,
                        gridFocusRequester = gridFocusRequester,
                        focusRequesters = focusRequesters,
                        mainMenuFocusRequester = mainMenuFocusRequester,
                        onLastFocusedIndexChanged = ::rememberFocusedItem,
                        gridHasFocus = gridHasFocus,
                        onGridHasFocusChanged = { gridHasFocus = it },
                        isRestoringFocus = isRestoringFocus,
                        onRestoreGridFocus = {
                            requestResultFocus(
                                index = restoreTargetIndex(),
                                fallbackFocusRequester = gridFallbackFocusRequester(),
                            )
                        },
                        gridFallbackFocusRequester = gridFallbackFocusRequester(),
                        onItemSelected = onItemSelected,
                    )
                }
            }
        }
    }
}

@Composable
private fun SearchErrorMessage(
    message: String,
    retryFocusRequester: FocusRequester,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.error,
        )
        Spacer(modifier = Modifier.height(12.dp))
        TvRetryButton(
            text = stringResource(R.string.search_retry),
            modifier = Modifier.focusRequester(retryFocusRequester),
            onClick = onRetry,
        )
    }
}

private fun SearchFilters.focusStateKey(): String = buildString {
    append("genres=")
    append(genres.sorted().joinToString(","))
    append("|excluded=")
    append(excludedGenres.sorted().joinToString(","))
    append("|types=")
    append(types.sorted().joinToString(","))
    append("|statuses=")
    append(statuses.sorted().joinToString(","))
    append("|from=")
    append(fromYear)
    append("|to=")
    append(toYear)
    append("|seasons=")
    append(seasons.sorted().joinToString(","))
    append("|ages=")
    append(ageRatings.sorted().joinToString(","))
    append("|sort=")
    append(sort.name)
    append("|forward=")
    append(sortForward)
}

private fun Throwable.uiMessage(): String =
    message ?: localizedMessage ?: toString()
