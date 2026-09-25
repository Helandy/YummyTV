package su.afk.yummy.tv.feature.reviews.mobile.list

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import kotlinx.coroutines.flow.Flow
import su.afk.yummy.tv.core.designsystem.baseScreen.BaseScreen
import su.afk.yummy.tv.core.designsystem.mobile.MobileSwipeableTabsPager
import su.afk.yummy.tv.core.designsystem.mobile.bar.MobileTopBar
import su.afk.yummy.tv.core.designsystem.mobile.layout.mobileContentMaxWidth
import su.afk.yummy.tv.core.designsystem.mobile.rememberMobileSwipeableTabsState
import su.afk.yummy.tv.core.designsystem.mobile.state.MobileAppendError
import su.afk.yummy.tv.core.designsystem.mobile.state.MobileStateContent
import su.afk.yummy.tv.domain.reviews.model.AnimeReviewSummary
import su.afk.yummy.tv.feature.reviews.list.ReviewsListState
import su.afk.yummy.tv.feature.reviews.mobile.R
import su.afk.yummy.tv.feature.reviews.mobile.view.ReviewMobileCard
import su.afk.yummy.tv.feature.reviews.mobile.view.ReviewsSortSelector

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewsListMobileScreen(
    state: ReviewsListState.State,
    effect: Flow<ReviewsListState.Effect>,
    onEvent: (ReviewsListState.Event) -> Unit
) {
    val context = LocalContext.current
    val items = state.reviews.collectAsLazyPagingItems()
    LaunchedEffect(Unit) {
        effect.collect {
            if (it is ReviewsListState.Effect.ShowToast) {
                Toast.makeText(context, it.message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    val sorts = state.availableSorts
    val tabsState = rememberMobileSwipeableTabsState(
        selectedPage = sorts.indexOf(state.sort).coerceAtLeast(0),
        pageCount = sorts.size.coerceAtLeast(1),
        onPageSelected = { page ->
            sorts.getOrNull(page)?.let { onEvent(ReviewsListState.Event.SortSelected(it)) }
        },
    )

    BaseScreen(
        contentModifier = Modifier.navigationBarsPadding(),
        isScroll = false,
        customTopBar = {
            MobileTopBar(
                title = stringResource(R.string.reviews_title),
                onBack = { onEvent(ReviewsListState.Event.BackSelected) },
            )
        },
    ) {
        if (sorts.isNotEmpty()) {
            ReviewsSortSelector(
                sorts = sorts,
                selectedSort = state.sort,
                onSortSelected = { sort -> tabsState.selectPage(sorts.indexOf(sort)) },
                modifier = Modifier.fillMaxWidth(),
            )
        }

        if (sorts.size > 1) {
            MobileSwipeableTabsPager(
                state = tabsState,
                modifier = Modifier.weight(1f),
                key = { page -> sorts[page].name },
            ) { _ ->
                ReviewsListContent(state, items, onEvent)
            }
        } else {
            ReviewsListContent(state, items, onEvent)
        }
    }
}

@Composable
private fun ReviewsListContent(
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
                key = items.itemKey { it.id },
            ) { index ->
                items[index]?.let { review ->
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
                    ) { CircularProgressIndicator() }
                }

                is LoadState.Error -> item {
                    MobileAppendError(
                        message = stringResource(R.string.reviews_error),
                        onRetry = { items.retry() },
                    )
                }

                else -> Unit
            }
        }
    }
}
