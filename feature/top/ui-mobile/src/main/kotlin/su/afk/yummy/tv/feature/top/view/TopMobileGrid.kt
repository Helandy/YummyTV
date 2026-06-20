package su.afk.yummy.tv.feature.top.view

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.itemKey
import su.afk.yummy.tv.core.designsystem.presenter.mobile.MobileBottomBarDefaults
import su.afk.yummy.tv.core.designsystem.presenter.mobile.MobileMessage
import su.afk.yummy.tv.core.designsystem.presenter.mobile.MobilePosterCard
import su.afk.yummy.tv.core.designsystem.presenter.mobile.MobilePosterGrid
import su.afk.yummy.tv.domain.top.model.AnimeTopItem
import su.afk.yummy.tv.feature.top.mobile.R

@Composable
internal fun TopMobileGrid(
    pagingItems: LazyPagingItems<AnimeTopItem>,
    isActive: Boolean,
    onAnimeSelected: (Int) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val gridState = rememberLazyGridState()
    val refreshState = pagingItems.loadState.refresh
    val appendState = pagingItems.loadState.append
    val itemCount = if (isActive) pagingItems.itemCount else 0
    val isLoading = !isActive || refreshState is LoadState.Loading
    val refreshError = (refreshState as? LoadState.Error)?.error?.uiMessage()
    val appendError = (appendState as? LoadState.Error)?.error?.uiMessage()

    if (isLoading && itemCount == 0) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator()
        }
        return
    }

    MobilePosterGrid(
        contentPadding = PaddingValues(
            bottom = MobileBottomBarDefaults.PosterGridContentBottomPadding +
                    MobileBottomBarDefaults.ExtraContentBottomPadding,
        ),
        modifier = modifier.fillMaxSize(),
        state = gridState,
    ) {
        when {
            refreshError != null && itemCount == 0 -> item(span = { GridItemSpan(maxLineSpan) }) {
                MobileMessage(
                    title = refreshError,
                    actionLabel = stringResource(R.string.top_mobile_retry),
                    onAction = onRetry,
                )
            }

            appendError != null -> item(span = { GridItemSpan(maxLineSpan) }) {
                Button(onClick = onRetry) {
                    Text(
                        stringResource(
                            R.string.top_mobile_retry_error,
                            appendError,
                        ),
                    )
                }
            }
        }

        items(
            count = itemCount,
            key = pagingItems.itemKey { it.id },
        ) { index ->
            pagingItems[index]?.let { item ->
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
        }

        if (appendState is LoadState.Loading && itemCount > 0) {
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

private fun Throwable.uiMessage(): String =
    message ?: localizedMessage ?: toString()
