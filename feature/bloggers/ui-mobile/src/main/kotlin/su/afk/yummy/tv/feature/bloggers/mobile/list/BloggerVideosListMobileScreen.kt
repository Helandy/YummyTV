package su.afk.yummy.tv.feature.bloggers.mobile.list

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import kotlinx.coroutines.flow.Flow
import su.afk.yummy.tv.core.designsystem.baseScreen.BaseScreen
import su.afk.yummy.tv.core.designsystem.mobile.MobileSwipeableTabsPager
import su.afk.yummy.tv.core.designsystem.mobile.bar.MobileTopBar
import su.afk.yummy.tv.core.designsystem.mobile.rememberMobileSwipeableTabsState
import su.afk.yummy.tv.core.designsystem.mobile.state.MobileAppendError
import su.afk.yummy.tv.core.designsystem.mobile.state.MobileStateContent
import su.afk.yummy.tv.core.utils.lazylist.lazyKey
import su.afk.yummy.tv.domain.bloggers.model.BloggerVideo
import su.afk.yummy.tv.domain.bloggers.model.BloggerVideoSort
import su.afk.yummy.tv.feature.bloggers.list.BloggerVideosListState
import su.afk.yummy.tv.feature.bloggers.mobile.R
import su.afk.yummy.tv.feature.bloggers.mobile.view.BloggerVideoMobileCard
import su.afk.yummy.tv.feature.bloggers.mobile.view.BloggerVideosFilters

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BloggerVideosListMobileScreen(
    state: BloggerVideosListState.State,
    effect: Flow<BloggerVideosListState.Effect>,
    onEvent: (BloggerVideosListState.Event) -> Unit,
) {
    val videos = state.videos.collectAsLazyPagingItems()
    val sorts = BloggerVideoSort.entries
    val tabsState = rememberMobileSwipeableTabsState(
        selectedPage = sorts.indexOf(state.sort).coerceAtLeast(0),
        pageCount = sorts.size,
        onPageSelected = { page ->
            sorts.getOrNull(page)?.let { onEvent(BloggerVideosListState.Event.SortSelected(it)) }
        },
    )
    BackHandler { onEvent(BloggerVideosListState.Event.BackSelected) }
    BaseScreen(
        isScroll = false,
        customTopBar = {
            MobileTopBar(
                title = stringResource(if (state.animeId == null) R.string.blogger_videos_title else R.string.blogger_videos_anime_title),
                onBack = { onEvent(BloggerVideosListState.Event.BackSelected) },
            )
        },
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .navigationBarsPadding()
        ) {
            if (state.animeId == null) {
                BloggerVideosFilters(
                    state = state,
                    onCategorySelected = {
                        onEvent(BloggerVideosListState.Event.CategorySelected(it))
                    },
                    onBloggerSelected = {
                        onEvent(BloggerVideosListState.Event.BloggerSelected(it))
                    },
                    onSortSelected = { sort ->
                        tabsState.selectPage(sorts.indexOf(sort))
                    },
                    onOpenBlogger = {
                        onEvent(BloggerVideosListState.Event.BloggerDetailsSelected(it))
                    },
                    onReset = {
                        onEvent(BloggerVideosListState.Event.FiltersReset)
                    },
                )
                MobileSwipeableTabsPager(
                    state = tabsState,
                    modifier = Modifier.weight(1f),
                    key = { page -> sorts[page].name },
                ) { _ ->
                    BloggerVideosListContent(videos, onEvent)
                }
            } else {
                BloggerVideosListContent(videos, onEvent)
            }
        }
    }
}

@Composable
private fun BloggerVideosListContent(
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
            modifier = Modifier.fillMaxSize(),
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
                    ) { CircularProgressIndicator() }
                }

                is LoadState.Error -> item {
                    MobileAppendError(
                        message = stringResource(R.string.blogger_videos_error),
                        onRetry = { videos.retry() },
                    )
                }

                else -> Unit
            }
        }
    }
}
