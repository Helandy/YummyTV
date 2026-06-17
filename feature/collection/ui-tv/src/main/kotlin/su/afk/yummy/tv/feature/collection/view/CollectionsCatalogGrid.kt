package su.afk.yummy.tv.feature.collection.view

import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
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
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import su.afk.yummy.tv.core.designsystem.presenter.components.TvTitleCard
import su.afk.yummy.tv.core.designsystem.presenter.components.loader.TvLoadingFooter
import su.afk.yummy.tv.core.designsystem.presenter.dimensions.TvCardSpacing
import su.afk.yummy.tv.core.designsystem.presenter.dimensions.TvScreenPadding
import su.afk.yummy.tv.core.designsystem.presenter.dimensions.currentTvTitleCardDimensions
import su.afk.yummy.tv.core.designsystem.presenter.locals.LocalMainMenuFocusRequester
import su.afk.yummy.tv.domain.collection.model.CollectionSummary
import su.afk.yummy.tv.feature.collection.R

@Composable
internal fun CollectionsCatalogGrid(
    items: List<CollectionSummary>,
    canLoadMore: Boolean,
    isLoading: Boolean,
    isLoadingMore: Boolean,
    itemFocusRequesters: List<FocusRequester>,
    onCollectionSelected: (Int) -> Unit,
    onCollectionFocused: (Int) -> Unit,
    onLoadMore: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val gridState = rememberLazyGridState()
    val mainMenuFocusRequester = LocalMainMenuFocusRequester.current
    val cardWidth = currentTvTitleCardDimensions().width
    val shouldLoadMore by remember(gridState, items.size, canLoadMore, isLoading, isLoadingMore) {
        derivedStateOf {
            val lastVisible = gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            val total = gridState.layoutInfo.totalItemsCount
            items.isNotEmpty() &&
                    canLoadMore &&
                    !isLoading &&
                    !isLoadingMore &&
                    total > 0 &&
                    lastVisible >= total - LOAD_MORE_THRESHOLD
        }
    }

    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) {
            onLoadMore()
        }
    }

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
                )
            }

            itemsIndexed(items, key = { _, item -> item.id }) { index, item ->
                TvTitleCard(
                    title = item.title,
                    posterUrl = item.posterUrl,
                    onClick = { onCollectionSelected(item.id) },
                    onFocused = { onCollectionFocused(item.id) },
                    modifier = Modifier
                        .focusRequester(itemFocusRequesters[index])
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

private const val LOAD_MORE_THRESHOLD = 4
private val CollectionLikeGreen = Color(0xFF4CAF50)
private val CollectionLikeBadgeBackground = Color.Black.copy(alpha = 0.62f)
