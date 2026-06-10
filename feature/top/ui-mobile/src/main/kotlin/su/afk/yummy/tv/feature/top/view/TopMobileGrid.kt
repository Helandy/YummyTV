package su.afk.yummy.tv.feature.top.view

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
    MobilePosterGrid(
        contentPadding = PaddingValues(),
        modifier = modifier.fillMaxSize(),
    ) {
        when {
            isLoading && items.isEmpty() -> item(span = { GridItemSpan(maxLineSpan) }) {
                Text(stringResource(R.string.top_mobile_loading))
            }

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

        if (canLoadMore && items.isNotEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Button(
                    onClick = onLoadMore,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        if (isLoadingMore) {
                            stringResource(R.string.top_mobile_loading)
                        } else {
                            stringResource(R.string.top_mobile_load_more)
                        },
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
