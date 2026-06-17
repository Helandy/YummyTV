package su.afk.yummy.tv.feature.collection.catalog

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.Flow
import su.afk.yummy.tv.core.designsystem.presenter.baseScreen.BaseScreen
import su.afk.yummy.tv.core.designsystem.presenter.mobile.MobileMessage
import su.afk.yummy.tv.core.designsystem.presenter.mobile.MobilePosterCard
import su.afk.yummy.tv.core.designsystem.presenter.mobile.MobilePosterGrid
import su.afk.yummy.tv.core.designsystem.presenter.mobile.MobileTopBar
import su.afk.yummy.tv.core.model.ErrorItem
import su.afk.yummy.tv.feature.collection.mobile.R

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun CollectionsCatalogMobileScreen(
    state: CollectionsCatalogState.State,
    effect: Flow<CollectionsCatalogState.Effect>,
    onEvent: (CollectionsCatalogState.Event) -> Unit,
) {
    val initialError = state.error?.takeIf { state.items.isEmpty() }
    val gridState = rememberLazyGridState()
    val shouldLoadMore by remember(
        gridState,
        state.items.size,
        state.canLoadMore,
        state.isLoading,
        state.isLoadingMore,
    ) {
        derivedStateOf {
            val lastVisible = gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            val total = gridState.layoutInfo.totalItemsCount
            state.items.isNotEmpty() &&
                    state.canLoadMore &&
                    !state.isLoading &&
                    !state.isLoadingMore &&
                    total > 0 &&
                    lastVisible >= total - LOAD_MORE_THRESHOLD
        }
    }

    LaunchedEffect(shouldLoadMore, state.items.size) {
        if (shouldLoadMore) {
            onEvent(CollectionsCatalogState.Event.LoadMoreSelected)
        }
    }

    BaseScreen(
        isScroll = false,
        topBar = {
            MobileTopBar(
                title = stringResource(R.string.collection_catalog_mobile_title),
                onBack = { onEvent(CollectionsCatalogState.Event.BackSelected) },
            )
        },
        isLoading = state.isLoading && state.items.isEmpty(),
        error = initialError?.let { ErrorItem(title = it, message = it) },
        onRetry = { onEvent(CollectionsCatalogState.Event.RetrySelected) },
        errorContent = initialError?.let { message ->
            { _, retry ->
                MobileMessage(
                    title = message,
                    actionLabel = stringResource(R.string.collection_mobile_retry),
                    onAction = retry,
                )
            }
        },
    ) {
        MobilePosterGrid(
            contentPadding = PaddingValues(0.dp),
            state = gridState,
        ) {
            val error = state.error
            if (error != null && state.items.isNotEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Text(error, color = MaterialTheme.colorScheme.error)
                }
            }
            items(state.items, key = { it.id }) { item ->
                MobilePosterCard(
                    title = item.title,
                    posterUrl = item.posterUrl,
                    posterOverlay = {
                        CollectionLikesBadge(
                            likesCount = item.likesCount,
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(6.dp),
                        )
                    },
                    onClick = {
                        onEvent(CollectionsCatalogState.Event.CollectionSelected(item.id))
                    },
                )
            }
            if (state.isLoadingMore) {
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
}

@Composable
private fun CollectionLikesBadge(
    likesCount: Int,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(Color.Black.copy(alpha = 0.62f))
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

private const val LOAD_MORE_THRESHOLD = 6
private val CollectionLikeGreen = Color(0xFF4CAF50)
