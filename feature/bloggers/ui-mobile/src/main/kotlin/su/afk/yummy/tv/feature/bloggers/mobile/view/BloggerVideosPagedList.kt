package su.afk.yummy.tv.feature.bloggers.mobile.view

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
import su.afk.yummy.tv.core.utils.lazylist.lazyKey
import su.afk.yummy.tv.domain.bloggers.model.BloggerVideo
import su.afk.yummy.tv.feature.bloggers.list.BloggerVideosListState
import su.afk.yummy.tv.feature.bloggers.mobile.view.BloggerVideoMobileCard
import su.afk.yummy.tv.feature.bloggers.presentation.R

@Composable
internal fun BloggerVideosPagedList(
    videos: LazyPagingItems<BloggerVideo>,
    onEvent: (BloggerVideosListState.Event) -> Unit,
) {
    val refresh = videos.loadState.refresh
    MobileStateContent(
        isLoading = refresh is LoadState.Loading,
        error = (refresh as? LoadState.Error)?.let { stringResource(R.string.blogger_videos_error) },
        onRetry = { videos.retry() },
        empty = videos.itemCount == 0,
        emptyText = stringResource(R.string.blogger_videos_empty),
    ) {
        LazyColumn(
            modifier = Modifier
                .mobileContentMaxWidth()
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            items(
                videos.itemCount,
                key = { index -> lazyKey("bloggervideo", videos[index]?.id, index) },
            ) { index ->
                videos[index]?.let { video ->
                    BloggerVideoMobileCard(
                        video,
                        { onEvent(BloggerVideosListState.Event.VideoSelected(video.id)) },
                        { onEvent(BloggerVideosListState.Event.BloggerDetailsSelected(video.creator.id)) },
                    )
                }
            }
            when (videos.loadState.append) {
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
                        message = stringResource(R.string.blogger_videos_error),
                        onRetry = { videos.retry() },
                    )
                }

                else -> Unit
            }
        }
    }
}
