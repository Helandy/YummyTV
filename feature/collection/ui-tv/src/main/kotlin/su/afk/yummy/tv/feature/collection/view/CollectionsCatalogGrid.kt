package su.afk.yummy.tv.feature.collection.view

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.gestures.LocalBringIntoViewSpec
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.itemKey
import su.afk.yummy.tv.core.designsystem.components.CollectionStatBadges
import su.afk.yummy.tv.core.designsystem.dimensions.TvCardSpacing
import su.afk.yummy.tv.core.designsystem.dimensions.TvScreenPadding
import su.afk.yummy.tv.core.designsystem.dimensions.currentTvTitleCardDimensions
import su.afk.yummy.tv.core.designsystem.dimensions.tvPosterGridColumns
import su.afk.yummy.tv.core.designsystem.focus.rememberTvGridStartExtent
import su.afk.yummy.tv.core.designsystem.focus.rememberTvTopAnchoredGridBringIntoViewSpec
import su.afk.yummy.tv.core.designsystem.focus.tvLazyGridRowFocusNavigation
import su.afk.yummy.tv.core.designsystem.focus.tvWholeItemBringIntoView
import su.afk.yummy.tv.core.designsystem.locals.LocalMainMenuFocusRequester
import su.afk.yummy.tv.core.designsystem.tv.TvChip
import su.afk.yummy.tv.core.designsystem.tv.TvCollectionCard
import su.afk.yummy.tv.core.designsystem.tv.TvLoadingFooter
import su.afk.yummy.tv.domain.collection.model.CollectionSort
import su.afk.yummy.tv.domain.collection.model.CollectionSummary
import su.afk.yummy.tv.feature.collection.presentation.R
import su.afk.yummy.tv.feature.collection.utils.labelRes

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun CollectionsCatalogGrid(
    pagingItems: LazyPagingItems<CollectionSummary>,
    isLoadingMore: Boolean,
    selectedSort: CollectionSort,
    sortFocusRequester: FocusRequester,
    onSortSelected: (CollectionSort) -> Unit,
    itemFocusRequesters: List<FocusRequester>,
    onCollectionSelected: (Int) -> Unit,
    onCollectionFocused: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val gridState = rememberLazyGridState()
    val scope = rememberCoroutineScope()
    val mainMenuFocusRequester = LocalMainMenuFocusRequester.current
    // Плоская карточка занимает ширину двух постерных колонок.
    val cardWidth = currentTvTitleCardDimensions().width * 2
    val itemCount = pagingItems.itemCount
    val gridStartExtent = rememberTvGridStartExtent(TvScreenPadding.Vertical, TvCardSpacing.Vertical)

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val horizontalSpacing = TvCardSpacing.Horizontal
        val gridColumnCount = tvPosterGridColumns(
            availableWidth = maxWidth - TvScreenPadding.Horizontal - TvScreenPadding.Horizontal,
            cardWidth = cardWidth,
            spacing = horizontalSpacing,
        )
                .coerceAtLeast(1)

        CompositionLocalProvider(
            LocalBringIntoViewSpec provides rememberTvTopAnchoredGridBringIntoViewSpec(TvCardSpacing.Vertical),
        ) {
            LazyVerticalGrid(
                state = gridState,
                columns = GridCells.Fixed(gridColumnCount),
                contentPadding = PaddingValues(
                    start = TvScreenPadding.Horizontal,
                    end = TvScreenPadding.Horizontal,
                    top = TvScreenPadding.Vertical,
                    bottom = TvScreenPadding.Vertical,
                ),
                verticalArrangement = Arrangement.spacedBy(TvCardSpacing.Vertical),
                horizontalArrangement = Arrangement.spacedBy(horizontalSpacing),
                modifier = Modifier
                    .fillMaxSize()
                    .focusGroup(),
            ) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(gridStartExtent.measure),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = stringResource(R.string.collection_catalog_title),
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Spacer(Modifier.weight(1f))
                        CollectionSort.entries.forEach { sort ->
                            TvChip(
                                label = stringResource(sort.labelRes()),
                                selected = selectedSort == sort,
                                onClick = { onSortSelected(sort) },
                                modifier = if (selectedSort == sort) {
                                    Modifier.focusRequester(sortFocusRequester)
                                } else {
                                    Modifier
                                },
                            )
                        }
                    }
                }

                items(
                    count = itemCount,
                    key = pagingItems.itemKey { it.id },
                ) { index ->
                    val item = pagingItems[index] ?: return@items
                    TvCollectionCard(
                        title = item.title,
                        posterUrls = item.posterUrls.ifEmpty { listOfNotNull(item.posterUrl) },
                        onClick = { onCollectionSelected(item.id) },
                        // Заголовок "Коллекции" над верхним рядом показывает
                        // tvBringIntoViewWithGridStart, подскролл вручную не нужен.
                        onFocused = { onCollectionFocused(item.id) },
                        badge = {
                            CollectionStatBadges(
                                likes = item.likesCount,
                                views = item.viewsCount,
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(8.dp),
                            )
                        },
                        modifier = Modifier
                            .focusRequester(itemFocusRequesters[index])
                            .tvWholeItemBringIntoView(gridStartExtent.takeIf { index < gridColumnCount })
                            .tvLazyGridRowFocusNavigation(
                                index = index,
                                columnCount = gridColumnCount,
                                itemCount = itemCount,
                                gridState = gridState,
                                scope = scope,
                                focusRequesterAt = itemFocusRequesters::getOrNull,
                                // нулевой lazy-индекс занимает шапка "Коллекции"
                                lazyIndexOffset = 1,
                            )
                            .onPreviewKeyEvent { event ->
                                if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                                if (event.key != Key.DirectionLeft) return@onPreviewKeyEvent false
                                if (index % gridColumnCount != 0) return@onPreviewKeyEvent false
                                runCatching { mainMenuFocusRequester?.requestFocus() }
                                mainMenuFocusRequester != null
                            },
                    )
                }

                if (isLoadingMore) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        TvLoadingFooter()
                    }
                }
            }
        }
    }
}
