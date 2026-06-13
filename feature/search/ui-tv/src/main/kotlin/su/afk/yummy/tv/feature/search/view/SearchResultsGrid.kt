package su.afk.yummy.tv.feature.search.view

import androidx.compose.foundation.focusGroup
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
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.core.designsystem.presenter.components.RatingBadge
import su.afk.yummy.tv.core.designsystem.presenter.components.TvTitleCard
import su.afk.yummy.tv.core.designsystem.presenter.components.loader.TvLoadingFooter
import su.afk.yummy.tv.core.designsystem.presenter.dimensions.TvCardSpacing
import su.afk.yummy.tv.core.designsystem.presenter.dimensions.TvScreenPadding
import su.afk.yummy.tv.core.designsystem.presenter.dimensions.currentTvTitleCardDimensions
import su.afk.yummy.tv.domain.anime.model.AnimePreview
import su.afk.yummy.tv.domain.search.model.SearchItem

@Composable
internal fun SearchResultsGrid(
    items: List<SearchItem>,
    isLoading: Boolean,
    focusedItemId: Int?,
    focusedPreview: AnimePreview?,
    gridState: LazyGridState,
    focusRequesters: List<FocusRequester>,
    mainMenuFocusRequester: FocusRequester?,
    onLastFocusedIndexChanged: (Int) -> Unit,
    gridHasFocus: Boolean,
    onGridHasFocusChanged: (Boolean) -> Unit,
    isRestoringFocus: Boolean,
    onRestoreGridFocus: () -> Unit,
    onItemSelected: (SearchItem) -> Unit,
    onItemFocused: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val cardWidth = currentTvTitleCardDimensions().width

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val horizontalSpacing = TvCardSpacing.Horizontal
        val gridColumnCount =
            (((maxWidth - TvScreenPadding.Horizontal - TvScreenPadding.Horizontal).value + horizontalSpacing.value) /
                    (cardWidth.value + horizontalSpacing.value)).toInt()
                .coerceAtLeast(1)
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
                .onFocusChanged { state ->
                    val hadFocus = gridHasFocus
                    onGridHasFocusChanged(state.hasFocus)
                    if (state.hasFocus && !hadFocus && items.isNotEmpty()) {
                        onRestoreGridFocus()
                    }
                }
                .focusGroup(),
        ) {
            itemsIndexed(items, key = { _, item -> item.id }) { index, item ->
                val stableOnClick = remember(item.id, index) {
                    {
                        onLastFocusedIndexChanged(index)
                        onItemFocused(item.id)
                        onItemSelected(item)
                    }
                }
                val stableOnFocused = remember(item.id) { { onItemFocused(item.id) } }
                TvTitleCard(
                    title = item.title,
                    posterUrl = item.posterUrl,
                    onClick = stableOnClick,
                    screenshotUrls = if (item.id == focusedItemId) {
                        focusedPreview?.screenshotUrls.orEmpty()
                    } else {
                        emptyList()
                    },
                    onFocused = stableOnFocused,
                    modifier = Modifier
                        .focusRequester(focusRequesters[index])
                        .onPreviewKeyEvent { event ->
                            if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                            if (event.key != Key.DirectionLeft) return@onPreviewKeyEvent false
                            if (index % gridColumnCount != 0) return@onPreviewKeyEvent false
                            runCatching { mainMenuFocusRequester?.requestFocus() }
                            mainMenuFocusRequester != null
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
            if (isLoading && items.isNotEmpty()) {
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
