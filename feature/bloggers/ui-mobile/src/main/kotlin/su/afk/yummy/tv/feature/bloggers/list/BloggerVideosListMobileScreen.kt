package su.afk.yummy.tv.feature.bloggers.list

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.Flow
import su.afk.yummy.tv.core.designsystem.presenter.mobile.MobileStateContent
import su.afk.yummy.tv.feature.bloggers.mobile.R
import su.afk.yummy.tv.feature.bloggers.view.BloggerVideoMobileCard
import su.afk.yummy.tv.feature.bloggers.view.BloggerVideosFilters

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BloggerVideosListMobileScreen(
    state: BloggerVideosListState.State,
    effect: Flow<BloggerVideosListState.Effect>,
    onEvent: (BloggerVideosListState.Event) -> Unit,
) {
    BackHandler { onEvent(BloggerVideosListState.Event.BackSelected) }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(if (state.animeId == null) R.string.blogger_videos_title else R.string.blogger_videos_anime_title)) },
                navigationIcon = {
                    IconButton(onClick = { onEvent(BloggerVideosListState.Event.BackSelected) }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            null
                        )
                    }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
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
                    onSortSelected = {
                        onEvent(BloggerVideosListState.Event.SortSelected(it))
                    },
                    onOpenBlogger = {
                        onEvent(BloggerVideosListState.Event.BloggerDetailsSelected(it))
                    },
                    onReset = {
                        onEvent(BloggerVideosListState.Event.FiltersReset)
                    },
                )
            }
            MobileStateContent(
                isLoading = state.isLoading,
                error = state.error?.let {
                    it.ifBlank { stringResource(R.string.blogger_videos_error) }
                },
                onRetry = { onEvent(BloggerVideosListState.Event.RetrySelected) },
                empty = state.videos.isEmpty(),
                emptyText = stringResource(R.string.blogger_videos_empty),
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    items(state.videos, key = { it.id }) { video ->
                        BloggerVideoMobileCard(
                            video,
                            { onEvent(BloggerVideosListState.Event.VideoSelected(video.id)) },
                            { onEvent(BloggerVideosListState.Event.BloggerDetailsSelected(video.creator.id)) },
                        )
                    }
                }
            }
        }
    }
}
