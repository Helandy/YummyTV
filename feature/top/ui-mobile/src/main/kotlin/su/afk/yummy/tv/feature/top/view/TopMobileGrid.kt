package su.afk.yummy.tv.feature.top.view

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import su.afk.yummy.tv.core.designsystem.presenter.mobile.MobileMessage
import su.afk.yummy.tv.core.designsystem.presenter.mobile.MobilePosterCard
import su.afk.yummy.tv.core.designsystem.presenter.mobile.MobilePosterGrid
import su.afk.yummy.tv.domain.top.model.AnimeTopItem
import su.afk.yummy.tv.feature.top.mobile.R

@Composable
internal fun TopMobileGrid(
    items: List<AnimeTopItem>,
    isLoading: Boolean,
    isLoadingMore: Boolean,
    error: String?,
    canLoadMore: Boolean,
    onAnimeSelected: (Int) -> Unit,
    onRetry: () -> Unit,
    onLoadMore: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val gridState = rememberLazyGridState()
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

    if (isLoading && items.isEmpty()) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator()
        }
        return
    }

    MobilePosterGrid(
        contentPadding = PaddingValues(),
        modifier = modifier.fillMaxSize(),
        state = gridState,
    ) {
        when {
            error != null && items.isEmpty() -> item(span = { GridItemSpan(maxLineSpan) }) {
                MobileMessage(
                    title = error,
                    actionLabel = stringResource(R.string.top_mobile_retry),
                    onAction = onRetry,
                )
            }

            error != null -> item(span = { GridItemSpan(maxLineSpan) }) {
                Button(onClick = onRetry) {
                    Text(
                        stringResource(
                            R.string.top_mobile_retry_error,
                            error,
                        ),
                    )
                }
            }
        }

        itemsIndexed(items, key = { _, item -> item.id }) { index, item ->
            MobilePosterCard(
                title = item.title,
                posterUrl = item.posterUrl,
                rating = item.rating,
                posterOverlay = {
                    TopMobileRankBadge(
                        rank = index + 1,
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(4.dp),
                    )
                },
                onClick = { onAnimeSelected(item.id) },
            )
        }

        if (isLoadingMore && items.isNotEmpty()) {
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

@Composable
private fun TopMobileRankBadge(
    rank: Int,
    modifier: Modifier = Modifier,
) {
    Text(
        text = "#$rank",
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        color = Color.White,
        modifier = modifier
            .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
            .padding(horizontal = 5.dp, vertical = 2.dp),
    )
}

private const val LOAD_MORE_THRESHOLD = 6
