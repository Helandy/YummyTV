package su.afk.yummy.tv.feature.catalog.mobile

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import su.afk.yummy.tv.core.designsystem.baseScreen.BaseScreen
import su.afk.yummy.tv.core.designsystem.components.ErrorBanner
import su.afk.yummy.tv.core.designsystem.components.YummyLoadingIndicator
import su.afk.yummy.tv.core.designsystem.error.uiMessage
import su.afk.yummy.tv.core.designsystem.mobile.bar.MobileBottomBarDefaults
import su.afk.yummy.tv.core.designsystem.mobile.cards.MobilePosterCard
import su.afk.yummy.tv.core.designsystem.mobile.cards.MobilePosterGrid
import su.afk.yummy.tv.core.designsystem.mobile.cards.MobilePosterYear
import su.afk.yummy.tv.core.designsystem.mobile.state.MobileMessage
import su.afk.yummy.tv.core.designsystem.mobile.state.MobileSectionLoading
import su.afk.yummy.tv.core.designsystem.preview.ScreenPreviewTheme
import su.afk.yummy.tv.core.designsystem.skeleton.MobilePosterCardSkeleton
import su.afk.yummy.tv.core.designsystem.skeleton.rememberSkeletonShimmer
import su.afk.yummy.tv.feature.catalog.CatalogState
import su.afk.yummy.tv.feature.catalog.mobile.view.CATALOG_SKELETON_ITEMS
import su.afk.yummy.tv.feature.catalog.mobile.view.CatalogMobileClearButton
import su.afk.yummy.tv.feature.catalog.mobile.view.CatalogMobileFilterButton
import su.afk.yummy.tv.feature.catalog.mobile.view.CatalogMobileFilterPanel
import su.afk.yummy.tv.feature.catalog.mobile.view.CatalogMobileSearchHistory
import su.afk.yummy.tv.feature.catalog.mobile.view.RandomAnimeFloatingButton
import su.afk.yummy.tv.feature.catalog.navigator.CatalogDestination
import su.afk.yummy.tv.feature.catalog.presentation.R
import su.afk.yummy.tv.core.designsystem.R as CoreR

@Preview(name = "Default", device = "spec:width=412dp,height=915dp,dpi=420", showBackground = true)
@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun CatalogMobileScreenDefaultPreview() =
    ScreenPreviewTheme {
        CatalogMobileScreen(CatalogDestination(), CatalogState.State(), emptyFlow()) {}
    }

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun CatalogMobileScreen(
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

    LaunchedEffect(state.isSearchFocusRequested) {
        if (!state.isSearchFocusRequested) return@LaunchedEffect
        gridState.scrollToItem(0)
        // Поле — первый элемент ленивой сетки: ждём кадр, пока оно встанет в композицию.
        withFrameNanos { }
        runCatching { queryFocusRequester.requestFocus() }
        keyboardController?.show()
        onEvent(CatalogState.Event.SearchFocusHandled)
    }

    BaseScreen(
        isScroll = false,
        floatingActionButtonEnd = {
            RandomAnimeFloatingButton(
                isLoading = state.isRandomAnimeLoading,
                onClick = { onEvent(CatalogState.Event.RandomAnimeSelected) },
            )
        },
        floatingActionButtonBottomPadding = 8.dp,
    ) {
        MobilePosterGrid(
            contentPadding = PaddingValues(bottom = MobileBottomBarDefaults.contentBottomPadding),
            modifier = Modifier.imePadding(),
            state = gridState,
        ) {
            item(span = { GridItemSpan(maxLineSpan) }, contentType = "query_field") {
                OutlinedTextField(
                    shape = MaterialTheme.shapes.large,
                    value = state.query,
                    onValueChange = { onEvent(CatalogState.Event.QueryChanged(it)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(queryFocusRequester),
                    placeholder = { Text(stringResource(R.string.search_placeholder)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = {
                        keyboardController?.hide()
                        onEvent(CatalogState.Event.SearchSubmitted)
                    }),
                    trailingIcon = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (state.query.isNotEmpty()) {
                                CatalogMobileClearButton(
                                    onClick = { onEvent(CatalogState.Event.QueryChanged("")) },
                                )
                            }
                            CatalogMobileFilterButton(
                                activeCount = state.filters.activeCount,
                                modifier = Modifier.padding(end = 6.dp),
                                onClick = {
                                    keyboardController?.hide()
                                    onEvent(CatalogState.Event.OpenFilters)
                                },
                            )
                        }
                    },
                )
            }
            item(span = { GridItemSpan(maxLineSpan) }, contentType = "submit_button") {
                Button(
                    onClick = {
                        keyboardController?.hide()
                        onEvent(CatalogState.Event.SearchSubmitted)
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        if (state.filters.activeCount > 0) {
                            stringResource(
                                R.string.search_mobile_submit_with_filters,
                                state.filters.activeCount,
                            )
                        } else {
                            stringResource(R.string.search_mobile_submit)
                        },
                    )
                }
            }
            if (state.query.isEmpty() && state.searchHistory.isNotEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }, contentType = "search_history") {
                    CatalogMobileSearchHistory(
                        queries = state.searchHistory,
                        onQuerySelected = {
                            keyboardController?.hide()
                            onEvent(CatalogState.Event.SearchHistorySelected(it))
                        },
                        onClear = { onEvent(CatalogState.Event.SearchHistoryCleared) },
                    )
                }
            }
            val isFirstPageLoading = state.isSearchPending ||
                (hasActiveSearch && refreshState is LoadState.Loading && results.itemCount == 0)
            // Лоадер внутри сетки: полноэкранный увёл бы поле поиска из-под пальца при наборе
            if (isFirstPageLoading && results.itemCount == 0) {
                items(count = CATALOG_SKELETON_ITEMS, contentType = { "poster_skeleton" }) {
                    MobilePosterCardSkeleton(
                        shimmer = rememberSkeletonShimmer(),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            } else if (isFirstPageLoading) {
                item(span = { GridItemSpan(maxLineSpan) }, contentType = "loading") {
                    MobileSectionLoading()
                }
            }
            if (hasActiveSearch && refreshState is LoadState.NotLoading && results.itemCount == 0) {
                item(span = { GridItemSpan(maxLineSpan) }, contentType = "empty_state") {
                    val hasFilters = state.filters.activeCount > 0
                    MobileMessage(
                        title = stringResource(R.string.search_empty),
                        icon = Icons.Filled.SearchOff,
                        actionLabel = stringResource(R.string.search_mobile_reset_filters)
                            .takeIf { hasFilters },
                        onAction = { onEvent(CatalogState.Event.ResetFilters) }.takeIf { hasFilters },
                        fillMaxSize = false,
                    )
                }
            }
            // Ошибка внутри сетки: экран корневой, строка поиска не должна пропадать
            if (initialError != null) {
                item(span = { GridItemSpan(maxLineSpan) }, contentType = "refresh_error") {
                    MobileMessage(
                        title = initialError,
                        actionLabel = stringResource(CoreR.string.common_retry),
                        onAction = {
                            onEvent(CatalogState.Event.RetrySelected)
                            results.retry()
                        },
                        fillMaxSize = false,
                    )
                }
            }
            val error = (appendState as? LoadState.Error)?.error
            if (error != null && results.itemCount > 0) {
                item(span = { GridItemSpan(maxLineSpan) }, contentType = "append_error") {
                    ErrorBanner(
                        message = error.uiMessage(),
                        onRetry = { results.retry() },
                    )
                }
            }
            items(
                count = results.itemCount,
                key = results.itemKey { it.id },
                contentType = { "poster" },
            ) { index ->
                results[index]?.let { item ->
                    MobilePosterCard(
                        title = item.title,
                        posterUrl = item.posterUrl,
                        animeId = item.id,
                        rating = item.rating,
                        posterOverlay = {
                            item.year?.takeIf { state.showTitleYear }?.let { MobilePosterYear(it) }
                        },
                        onClick = { onEvent(CatalogState.Event.ItemSelected(item.id)) },
                    )
                }
            }
            if (appendState is LoadState.Loading && results.itemCount > 0) {
                item(span = { GridItemSpan(maxLineSpan) }, contentType = "append_loader") {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center,
                    ) {
                        YummyLoadingIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    }
                }
            }
        }
    }

    if (state.isFilterPanelOpen) {
        CatalogMobileFilterPanel(
            draftFilters = state.draftFilters,
            filterOptions = state.filterOptions,
            isLoadingFilterOptions = state.isLoadingFilterOptions,
            onClose = { onEvent(CatalogState.Event.ApplyFilters) },
            onReset = { onEvent(CatalogState.Event.ResetDraftFilters) },
            onGenreToggled = { onEvent(CatalogState.Event.GenreToggled(it)) },
            onExcludedGenreToggled = { onEvent(CatalogState.Event.ExcludedGenreToggled(it)) },
            onTypeToggled = { onEvent(CatalogState.Event.TypeToggled(it)) },
            onStatusToggled = { onEvent(CatalogState.Event.StatusToggled(it)) },
            onSeasonToggled = { onEvent(CatalogState.Event.SeasonToggled(it)) },
            onAgeRatingToggled = { onEvent(CatalogState.Event.AgeRatingToggled(it)) },
            onFromYearChanged = { onEvent(CatalogState.Event.FromYearChanged(it)) },
            onToYearChanged = { onEvent(CatalogState.Event.ToYearChanged(it)) },
            onSortSelected = { onEvent(CatalogState.Event.SortSelected(it)) },
            onSortDirectionToggled = { onEvent(CatalogState.Event.SortDirectionToggled) },
        )
    }
}
