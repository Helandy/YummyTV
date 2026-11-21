package su.afk.yummy.tv.feature.posts.mobile.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import su.afk.yummy.tv.core.designsystem.R as CoreR
import su.afk.yummy.tv.core.designsystem.components.ErrorBanner
import su.afk.yummy.tv.core.designsystem.components.StateMessage
import su.afk.yummy.tv.core.designsystem.mobile.layout.mobileContentMaxWidth
import su.afk.yummy.tv.core.designsystem.mobile.state.MobileSectionLoading
import su.afk.yummy.tv.core.designsystem.paging.getOrNull
import su.afk.yummy.tv.core.designsystem.paging.safeItemKey
import su.afk.yummy.tv.core.utils.lazylist.lazyKey
import su.afk.yummy.tv.domain.posts.model.PostSummary
import su.afk.yummy.tv.feature.posts.list.PostsListState
import su.afk.yummy.tv.feature.posts.mobile.view.PostMobileCard
import su.afk.yummy.tv.feature.posts.mobile.view.PostsLoadingState
import su.afk.yummy.tv.feature.posts.presentation.R

@Composable
internal fun PostsList(
    posts: LazyPagingItems<PostSummary>,
    onEvent: (PostsListState.Event) -> Unit,
) {
    // Индикатор жеста держим до конца именно этой перезагрузки, иначе он гаснет сразу.
    var isPullRefreshing by remember { mutableStateOf(false) }
    val isRefreshLoading = posts.loadState.refresh is LoadState.Loading
    LaunchedEffect(isRefreshLoading) {
        if (!isRefreshLoading) isPullRefreshing = false
    }
    PullToRefreshBox(
        isRefreshing = isPullRefreshing,
        onRefresh = {
            isPullRefreshing = true
            posts.refresh()
        },
        modifier = Modifier.fillMaxSize(),
    ) {
    LazyColumn(
        modifier = Modifier
            .mobileContentMaxWidth()
            .fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        when {
            // При обновлении поверх показанного списка его не прячем: крутится индикатор жеста.
            isRefreshLoading && posts.itemCount == 0 -> item {
                PostsLoadingState()
            }

            posts.loadState.refresh is LoadState.Error -> item {
                StateMessage(
                    stringResource(R.string.posts_error),
                    fillMaxSize = false,
                    actionLabel = stringResource(CoreR.string.common_retry),
                    onAction = posts::retry
                )
            }

            posts.itemCount == 0 -> item {
                StateMessage(
                    stringResource(R.string.posts_empty),
                    fillMaxSize = false
                )
            }

            else -> items(posts.itemCount, key = posts.safeItemKey("post") { lazyKey("post", it.id) }) { index ->
                posts.getOrNull(index)?.let { post ->
                    PostMobileCard(
                        post,
                        { onEvent(PostsListState.Event.PostSelected(post.id)) })
                }
            }
        }
        if (posts.loadState.append is LoadState.Loading) {
            item { MobileSectionLoading(minHeight = 72.dp) }
        }
        if (posts.loadState.append is LoadState.Error) {
            item {
                ErrorBanner(
                    message = stringResource(R.string.posts_error),
                    onRetry = posts::retry,
                )
            }
        }
    }
    }
}
