package su.afk.yummy.tv.feature.reviews.mobile.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import su.afk.yummy.tv.core.designsystem.components.ErrorBanner
import su.afk.yummy.tv.core.designsystem.components.YummyLoadingIndicator
import su.afk.yummy.tv.core.designsystem.mobile.layout.mobileContentMaxWidth
import su.afk.yummy.tv.core.designsystem.mobile.state.MobileStateContent
import su.afk.yummy.tv.core.designsystem.paging.getOrNull
import su.afk.yummy.tv.core.designsystem.paging.safeItemKey
import su.afk.yummy.tv.domain.reviews.model.AnimeReviewSummary
import su.afk.yummy.tv.feature.reviews.list.ReviewsListState
import su.afk.yummy.tv.feature.reviews.mobile.view.ReviewMobileCard
import su.afk.yummy.tv.feature.reviews.presentation.R

@Composable
internal fun ReviewsPagedList(
    state: ReviewsListState.State,
    items: LazyPagingItems<AnimeReviewSummary>,
    onEvent: (ReviewsListState.Event) -> Unit,
) {
    val refresh = items.loadState.refresh
    MobileStateContent(
        isLoading = refresh is LoadState.Loading,
        error = (refresh as? LoadState.Error)?.let { stringResource(R.string.reviews_error) },
        onRetry = { items.retry() },
        empty = items.itemCount == 0,
        emptyText = stringResource(R.string.reviews_empty),
    ) {
        LazyColumn(
            modifier = Modifier
                .mobileContentMaxWidth()
                .fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                top = 12.dp,
                end = 16.dp,
                bottom = 104.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            items(
                items.itemCount,
                key = items.safeItemKey("review") { it.id },
            ) { index ->
                items.getOrNull(index)?.let { review ->
                    ReviewMobileCard(
                        review,
                        state.reactionOverrides[review.id] ?: review.reactions,
                        state.isGeneralFeed,
                        { onEvent(ReviewsListState.Event.ReviewSelected(review.id)) },
                        { onEvent(ReviewsListState.Event.AuthorSelected(review.author.id)) },
                        { onEvent(ReviewsListState.Event.VoteSelected(review, it)) },
                    )
                }
            }
            when (items.loadState.append) {
                is LoadState.Loading -> item {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center,
                    ) { YummyLoadingIndicator() }
                }

                is LoadState.Error -> item {
                    ErrorBanner(
                        message = stringResource(R.string.reviews_error),
                        onRetry = { items.retry() },
                    )
                }

                else -> Unit
            }
        }
    }
}
