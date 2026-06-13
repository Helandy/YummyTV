package su.afk.yummy.tv.feature.search.view

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.Job
import su.afk.yummy.tv.core.designsystem.presenter.components.loader.TvLoadingScreen
import su.afk.yummy.tv.core.designsystem.presenter.focus.launchTvLazyGridItemFocusRestore
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
    items: List<SearchItem>,
    isLoading: Boolean,
    canLoadMore: Boolean,
    focusedItemId: Int?,
    filters: SearchFilters,
    draftFilters: SearchFilters,
    filterOptions: SearchFilterOptions,
    isFilterPanelOpen: Boolean,
    isLoadingFilterOptions: Boolean,
    onQueryChanged: (String) -> Unit,
    onSearchSubmitted: () -> Unit,
    onItemSelected: (SearchItem) -> Unit,
    onItemFocused: (Int) -> Unit,
    onLoadMore: () -> Unit,
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
    restoreFocusedItemOnEnter: Boolean = false,
    onFocusedItemRestoreHandled: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val gridState = rememberLazyGridState()
    val shouldLoadMore by remember {
        derivedStateOf {
            val lastVisible = gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            val total = gridState.layoutInfo.totalItemsCount
            canLoadMore && total > 0 && lastVisible >= total - 6
        }
    }
    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) onLoadMore()
    }

    val itemIds = remember(items) { items.map { it.id } }
    val focusRequesters = remember(itemIds) { List(items.size) { FocusRequester() } }
    val searchFieldFocusRequester = remember { FocusRequester() }
    val filterButtonFocusRequester = remember { FocusRequester() }
    val registerPreferredContentFocusRequester = LocalPreferredContentFocusRequester.current
    val mainMenuFocusRequester = LocalMainMenuFocusRequester.current
    val filterPanelInitialFocusRequester = remember { FocusRequester() }
    val focusedItemIndex = focusedItemId?.let { id -> items.indexOfFirst { it.id == id } } ?: -1
    val focusStateKey = remember(query, filters) { "${query.trim()}|${filters.focusStateKey()}" }
    var lastFocusedIndex by rememberSaveable(focusStateKey) {
        val idx = focusedItemIndex.coerceAtLeast(0)
        mutableIntStateOf(idx)
    }
    var lastFocusedItemId by rememberSaveable(focusStateKey) {
        mutableStateOf(focusedItemId?.takeIf { it in itemIds })
    }
    var gridHasFocus by remember { mutableStateOf(false) }
    var isRestoringFocus by remember { mutableStateOf(false) }
    var restoreFilterButtonFocusToken by rememberSaveable { mutableIntStateOf(0) }
    var restoreFocusedItemToken by rememberSaveable { mutableIntStateOf(0) }
    var restoreFocusJob by remember { mutableStateOf<Job?>(null) }
    val currentRestoreFocusedItemOnEnter by rememberUpdatedState(restoreFocusedItemOnEnter)
    val currentHasResults by rememberUpdatedState(items.isNotEmpty())

    fun currentIndexFor(itemId: Int?): Int? =
        itemId?.let(itemIds::indexOf)?.takeIf { it >= 0 }

    fun rememberFocusedItem(index: Int) {
        lastFocusedIndex = index
        lastFocusedItemId = itemIds.getOrNull(index)
    }

    fun restoreTargetIndex(): Int {
        if (items.isEmpty()) return 0
        return (
                currentIndexFor(focusedItemId)
                    ?: currentIndexFor(lastFocusedItemId)
                    ?: lastFocusedIndex
                ).coerceIn(0, items.lastIndex)
    }

    fun requestResultFocus(
        index: Int,
        fallbackFocusRequester: FocusRequester = searchFieldFocusRequester,
        clearPendingRestore: Boolean = false,
    ) {
        if (items.isEmpty()) return
        val target = index.coerceIn(0, items.lastIndex)
        rememberFocusedItem(target)
        isRestoringFocus = true
        restoreFocusJob = launchTvLazyGridItemFocusRestore(
            previousJob = restoreFocusJob,
            scope = scope,
            itemIndex = target,
            gridState = gridState,
            itemFocusRequesters = focusRequesters,
            fallbackFocusRequester = fallbackFocusRequester,
            onRestoreFinished = {
                isRestoringFocus = false
                if (clearPendingRestore) {
                    onFocusedItemRestoreHandled()
                }
            },
        )
    }

    LaunchedEffect(focusedItemId, items) {
        currentIndexFor(focusedItemId)?.let { focusedIndex ->
            rememberFocusedItem(focusedIndex)
        }
    }

    val preferredContentFocusRequester = when {
        isFilterPanelOpen -> filterPanelInitialFocusRequester
        restoreFilterButtonFocusToken > 0 -> filterButtonFocusRequester
        restoreFocusedItemOnEnter && items.isNotEmpty() ->
            focusRequesters.getOrNull(restoreTargetIndex())

        else -> searchFieldFocusRequester
    }

    DisposableEffect(preferredContentFocusRequester, registerPreferredContentFocusRequester) {
        registerPreferredContentFocusRequester?.invoke(preferredContentFocusRequester)
        onDispose { registerPreferredContentFocusRequester?.invoke(null) }
    }

    DisposableEffect(Unit) {
        onDispose { restoreFocusJob?.cancel() }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (
                event == Lifecycle.Event.ON_RESUME &&
                currentRestoreFocusedItemOnEnter &&
                currentHasResults
            ) {
                restoreFocusedItemToken += 1
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(restoreFocusedItemToken, focusedItemIndex, items, focusRequesters) {
        if (
            restoreFocusedItemToken <= 0 ||
            !restoreFocusedItemOnEnter ||
            items.isEmpty()
        ) {
            return@LaunchedEffect
        }
        requestResultFocus(
            index = restoreTargetIndex(),
            fallbackFocusRequester = searchFieldFocusRequester,
            clearPendingRestore = true,
        )
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

    // Lift the focused card's row to the top once focus settles. A cancellable
    // effect keeps the focused row pinned so the row below stays composed and
    // DPAD-down preserves the column.
    LaunchedEffect(lastFocusedIndex, gridHasFocus) {
        if (gridHasFocus && !isRestoringFocus && items.isNotEmpty()) {
            gridState.scrollToItem(lastFocusedIndex.coerceIn(0, items.lastIndex))
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        SearchTvHeaderRow(
            query = query,
            filters = filters,
            searchFieldFocusRequester = searchFieldFocusRequester,
            filterButtonFocusRequester = filterButtonFocusRequester,
            mainMenuFocusRequester = mainMenuFocusRequester,
            onQueryChanged = onQueryChanged,
            onSearchSubmitted = onSearchSubmitted,
            onOpenFilters = onOpenFilters,
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
                items.isEmpty() && isLoading -> TvLoadingScreen()
                items.isEmpty() && (query.isNotBlank() || !filters.isEmpty) -> {
                    Text(
                        text = stringResource(R.string.search_empty),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.align(Alignment.Center),
                    )
                }
                else -> {
                    SearchResultsGrid(
                        items = items,
                        isLoading = isLoading,
                        focusedItemId = focusedItemId,
                        gridState = gridState,
                        focusRequesters = focusRequesters,
                        mainMenuFocusRequester = mainMenuFocusRequester,
                        onLastFocusedIndexChanged = ::rememberFocusedItem,
                        gridHasFocus = gridHasFocus,
                        onGridHasFocusChanged = { gridHasFocus = it },
                        isRestoringFocus = isRestoringFocus,
                        onRestoreGridFocus = {
                            requestResultFocus(
                                index = restoreTargetIndex(),
                                fallbackFocusRequester = searchFieldFocusRequester,
                                clearPendingRestore = restoreFocusedItemOnEnter,
                            )
                        },
                        onItemSelected = onItemSelected,
                        onItemFocused = onItemFocused,
                    )
                }
            }
        }
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
