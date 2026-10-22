package su.afk.yummy.tv.feature.catalog.view

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.LocalBringIntoViewSpec
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.unit.dp
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.itemKey
import su.afk.yummy.tv.core.designsystem.dimensions.TvCardSpacing
import su.afk.yummy.tv.core.designsystem.dimensions.TvScreenPadding
import su.afk.yummy.tv.core.designsystem.dimensions.currentTvTitleCardDimensions
import su.afk.yummy.tv.core.designsystem.dimensions.tvPosterGridColumns
import su.afk.yummy.tv.core.designsystem.focus.TvTopAnchoredExtraRowGap
import su.afk.yummy.tv.core.designsystem.focus.rememberTvTopAnchoredGridBringIntoViewSpec
import su.afk.yummy.tv.core.designsystem.focus.tvFocusRestorer
import su.afk.yummy.tv.core.designsystem.focus.tvLazyGridRowFocusNavigation
import su.afk.yummy.tv.core.designsystem.focus.tvWholeItemBringIntoView
import su.afk.yummy.tv.core.designsystem.tv.TvLoadingFooter
import su.afk.yummy.tv.core.designsystem.tv.TvPosterRating
import su.afk.yummy.tv.core.designsystem.tv.TvPosterYear
import su.afk.yummy.tv.core.designsystem.tv.TvTitleCard
import su.afk.yummy.tv.core.designsystem.tv.TvTitleRowCard
import su.afk.yummy.tv.domain.catalog.model.SearchItem

private val GridTopContentPadding = 8.dp

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun CatalogResultsGrid(
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
    showTitleYear: Boolean,
    isRowLayout: Boolean,
    modifier: Modifier = Modifier,
) {
    val cardWidth = currentTvTitleCardDimensions().width
    val itemCount = results.itemCount
    val scope = rememberCoroutineScope()

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val horizontalSpacing = TvCardSpacing.Horizontal
        // Построчные карточки — одна колонка на всю ширину.
        val gridColumnCount = if (isRowLayout) {
            1
        } else {
            tvPosterGridColumns(
                availableWidth = maxWidth - TvScreenPadding.Horizontal - TvScreenPadding.Horizontal,
                cardWidth = cardWidth,
                spacing = horizontalSpacing,
            ).coerceAtLeast(1)
        }
        CompositionLocalProvider(
            LocalBringIntoViewSpec provides rememberTvTopAnchoredGridBringIntoViewSpec(
                rowSpacing = TvCardSpacing.Vertical,
            ),
        ) {
            LazyVerticalGrid(
                state = gridState,
                columns = GridCells.Fixed(gridColumnCount),
                contentPadding = PaddingValues(
                    start = TvScreenPadding.Horizontal,
                    end = TvScreenPadding.Horizontal,
                    top = GridTopContentPadding,
                    bottom = TvScreenPadding.Vertical,
                ),
                horizontalArrangement = Arrangement.spacedBy(horizontalSpacing),
                verticalArrangement = Arrangement.spacedBy(TvCardSpacing.Vertical + TvTopAnchoredExtraRowGap),
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
                    val cardModifier = Modifier
                        .focusRequester(focusRequesters[index])
                        .tvWholeItemBringIntoView()
                        .tvLazyGridRowFocusNavigation(
                            index = index,
                            columnCount = gridColumnCount,
                            itemCount = itemCount,
                            gridState = gridState,
                            scope = scope,
                            focusRequesterAt = focusRequesters::getOrNull,
                        )
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
                        }
                    if (isRowLayout) {
                        TvTitleRowCard(
                            title = item.title,
                            posterUrl = item.posterUrl,
                            description = item.description,
                            year = item.year,
                            rating = item.rating,
                            animeId = item.id,
                            onClick = stableOnClick,
                            onFocused = stableOnFocused,
                            modifier = cardModifier,
                        )
                    } else {
                        TvTitleCard(
                            title = item.title,
                            posterUrl = item.posterUrl,
                            animeId = item.id,
                            onClick = stableOnClick,
                            onFocused = stableOnFocused,
                            modifier = cardModifier,
                            posterOverlay = {
                                item.rating?.let { TvPosterRating(it) }
                                item.year?.takeIf { showTitleYear }?.let { TvPosterYear(it) }
                            },
                        )
                    }
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
