package su.afk.yummy.tv.feature.collection.view

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.gestures.LocalBringIntoViewSpec
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.itemKey
import su.afk.yummy.tv.core.designsystem.dimensions.TvCardSpacing
import su.afk.yummy.tv.core.designsystem.dimensions.TvScreenPadding
import su.afk.yummy.tv.core.designsystem.dimensions.currentTvTitleCardDimensions
import su.afk.yummy.tv.core.designsystem.focus.rememberTvGridStartExtent
import su.afk.yummy.tv.core.designsystem.focus.rememberTvTopAnchoredGridBringIntoViewSpec
import su.afk.yummy.tv.core.designsystem.focus.tvLazyGridRowFocusNavigation
import su.afk.yummy.tv.core.designsystem.focus.tvWholeItemBringIntoView
import su.afk.yummy.tv.core.designsystem.locals.LocalMainMenuFocusRequester
import su.afk.yummy.tv.core.designsystem.theme.YummySemanticColors
import su.afk.yummy.tv.core.designsystem.tv.TvLoadingFooter
import su.afk.yummy.tv.core.designsystem.tv.TvTitleCard
import su.afk.yummy.tv.domain.collection.model.CollectionSummary
import su.afk.yummy.tv.feature.collection.R

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun CollectionsCatalogGrid(
    pagingItems: LazyPagingItems<CollectionSummary>,
    isLoadingMore: Boolean,
    itemFocusRequesters: List<FocusRequester>,
    onCollectionSelected: (Int) -> Unit,
    onCollectionFocused: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val gridState = rememberLazyGridState()
    val scope = rememberCoroutineScope()
    val mainMenuFocusRequester = LocalMainMenuFocusRequester.current
    val cardWidth = currentTvTitleCardDimensions().width
    val itemCount = pagingItems.itemCount
    val gridStartExtent = rememberTvGridStartExtent(TvScreenPadding.Vertical, TvCardSpacing.Vertical)

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val horizontalSpacing = TvCardSpacing.Horizontal
        val gridColumnCount =
            (((maxWidth - TvScreenPadding.Horizontal - TvScreenPadding.Horizontal).value + horizontalSpacing.value) /
                    (cardWidth.value + horizontalSpacing.value)).toInt()
                .coerceAtLeast(1)

        CompositionLocalProvider(
            LocalBringIntoViewSpec provides rememberTvTopAnchoredGridBringIntoViewSpec(TvCardSpacing.Vertical),
        ) {
            LazyVerticalGrid(
                state = gridState,
                columns = GridCells.Adaptive(minSize = cardWidth),
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
                    Text(
                        text = stringResource(R.string.collection_catalog_tv_title),
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = gridStartExtent.measure,
                    )
                }

                items(
                    count = itemCount,
                    key = pagingItems.itemKey { it.id },
                ) { index ->
                    val item = pagingItems[index] ?: return@items
                    TvTitleCard(
                        title = item.title,
                        posterUrl = item.posterUrl,
                        onClick = { onCollectionSelected(item.id) },
                        // Заголовок "Коллекции" над верхним рядом показывает
                        // tvBringIntoViewWithGridStart, подскролл вручную не нужен.
                        onFocused = { onCollectionFocused(item.id) },
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
                        posterOverlay = {
                            CollectionCatalogLikesBadge(
                                likesCount = item.likesCount,
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(6.dp),
                            )
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

@Composable
private fun CollectionCatalogLikesBadge(
    likesCount: Int,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(CollectionLikeBadgeBackground)
            .padding(horizontal = 6.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(
            imageVector = Icons.Filled.ThumbUp,
            contentDescription = null,
            tint = CollectionLikeGreen,
            modifier = Modifier.size(14.dp),
        )
        Text(
            text = likesCount.toString(),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = CollectionLikeGreen,
        )
    }
}

private val CollectionLikeGreen = YummySemanticColors.InProgress
private val CollectionLikeBadgeBackground = Color.Black.copy(alpha = 0.62f)
