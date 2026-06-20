package su.afk.yummy.tv.feature.search.view

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.LocalBringIntoViewSpec
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.unit.dp
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.itemKey
import su.afk.yummy.tv.core.designsystem.presenter.components.RatingBadge
import su.afk.yummy.tv.core.designsystem.presenter.components.TvTitleCard
import su.afk.yummy.tv.core.designsystem.presenter.components.loader.TvLoadingFooter
import su.afk.yummy.tv.core.designsystem.presenter.dimensions.TvCardSpacing
import su.afk.yummy.tv.core.designsystem.presenter.dimensions.TvScreenPadding
import su.afk.yummy.tv.core.designsystem.presenter.dimensions.currentTvTitleCardDimensions
import su.afk.yummy.tv.core.designsystem.presenter.focus.TvFocusedGridBringIntoViewSpec
import su.afk.yummy.tv.core.designsystem.presenter.focus.tvFocusRestorer
import su.afk.yummy.tv.domain.search.model.SearchItem

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun SearchResultsGrid(
    results: LazyPagingItems<SearchItem>,
    isLoading: Boolean,
    gridState: LazyGridState,
    gridFocusRequester: FocusRequester,
    focusRequesters: List<FocusRequester>,
    mainMenuFocusRequester: FocusRequester?,
    onLastFocusedIndexChanged: (Int) -> Unit,
    gridHasFocus: Boolean,
    onGridHasFocusChanged: (Boolean) -> Unit,
    isRestoringFocus: Boolean,
    onRestoreGridFocus: () -> Unit,
    gridFallbackFocusRequester: FocusRequester,
    onItemSelected: (SearchItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    val cardWidth = currentTvTitleCardDimensions().width
    val itemCount = results.itemCount

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val horizontalSpacing = TvCardSpacing.Horizontal
        val gridColumnCount =
            (((maxWidth - TvScreenPadding.Horizontal - TvScreenPadding.Horizontal).value + horizontalSpacing.value) /
                    (cardWidth.value + horizontalSpacing.value)).toInt()
                .coerceAtLeast(1)
        CompositionLocalProvider(
            LocalBringIntoViewSpec provides TvFocusedGridBringIntoViewSpec,
        ) {
            LazyVerticalGrid(
                state = gridState,
                columns = GridCells.Adaptive(minSize = cardWidth),
                contentPadding = PaddingValues(
                    start = TvScreenPadding.Horizontal,
                    end = TvScreenPadding.Horizontal,
                    top = 8.dp,
                    bottom = TvScreenPadding.Vertical,
                ),
                horizontalArrangement = Arrangement.spacedBy(horizontalSpacing),
                verticalArrangement = Arrangement.spacedBy(TvCardSpacing.Vertical),
                modifier = Modifier
                    .fillMaxSize()
                    .focusRequester(gridFocusRequester)
                    .tvFocusRestorer(
                        fallback = gridFallbackFocusRequester,
                        enabled = itemCount > 0,
                    )
                    .onFocusChanged { state ->
                        val hadFocus = gridHasFocus
                        onGridHasFocusChanged(state.hasFocus)
                        if (state.isFocused && !hadFocus && itemCount > 0 && !isRestoringFocus) {
                            onRestoreGridFocus()
                        }
                    }
                    .focusable(),
            ) {
                items(
                    count = itemCount,
                    key = results.itemKey { it.id },
                ) { index ->
                    val item = results[index] ?: return@items
                    val stableOnClick = remember(item.id, index) {
                        {
                            onLastFocusedIndexChanged(index)
                            onItemSelected(item)
                        }
                    }
                    val stableOnFocused =
                        remember(item.id, index) { { onLastFocusedIndexChanged(index) } }
                    TvTitleCard(
                        title = item.title,
                        posterUrl = item.posterUrl,
                        onClick = stableOnClick,
                        onFocused = stableOnFocused,
                        modifier = Modifier
                            .focusRequester(focusRequesters[index])
                            .focusProperties {
                                if (index % gridColumnCount == 0) {
                                    mainMenuFocusRequester?.let { left = it }
                                }
                            }
                            .onFocusChanged { state ->
                                if (state.hasFocus) {
                                    if (!isRestoringFocus) {
                                        onLastFocusedIndexChanged(index)
                                    }
                                }
                            },
                        posterOverlay = item.rating?.let { rating ->
                            {
                                RatingBadge(
                                    rating = rating,
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(4.dp),
                                )
                            }
                        },
                    )
                }
                if (isLoading && itemCount > 0) {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center,
                        ) {
                            TvLoadingFooter()
                        }
                    }
                }
            }
        }
    }
}
