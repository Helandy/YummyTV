package su.afk.yummy.tv.feature.reviews.mobile.list

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.paging.compose.collectAsLazyPagingItems
import kotlinx.coroutines.flow.Flow
import su.afk.yummy.tv.core.designsystem.presenter.baseScreen.BaseScreen
import su.afk.yummy.tv.core.designsystem.presenter.mobile.MobileAppendError
import su.afk.yummy.tv.core.designsystem.presenter.mobile.MobileStateContent
import su.afk.yummy.tv.core.designsystem.presenter.mobile.MobileTopBar
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
    LaunchedEffect(effect) {
        effect.collect {
            if (it is ReviewsListState.Effect.ShowToast) {
                Toast.makeText(context, it.message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    BaseScreen(
        isScroll = false,
        customTopBar = {
            MobileTopBar(
                title = stringResource(R.string.reviews_title),
                onBack = { onEvent(ReviewsListState.Event.BackSelected) },
            )
        },
    ) {
        if (state.availableSorts.isNotEmpty()) {
            ReviewsSortSelector(
                sorts = state.availableSorts,
                selectedSort = state.sort,
                onSortSelected = { onEvent(ReviewsListState.Event.SortSelected(it)) },
                modifier = Modifier.fillMaxWidth(),
            )
        }

        val refresh = items.loadState.refresh
        MobileStateContent(
            isLoading = refresh is LoadState.Loading,
            error = (refresh as? LoadState.Error)?.let { stringResource(R.string.reviews_error) },
            onRetry = { items.retry() },
            empty = items.itemCount == 0,
            emptyText = stringResource(R.string.reviews_empty),
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
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
                    key = { index -> items[index]?.id ?: index },
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
                when (val append = items.loadState.append) {
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
}
