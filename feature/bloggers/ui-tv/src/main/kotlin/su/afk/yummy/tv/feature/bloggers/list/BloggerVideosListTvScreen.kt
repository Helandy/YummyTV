package su.afk.yummy.tv.feature.bloggers.list

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.OndemandVideo
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import kotlinx.coroutines.flow.Flow
import su.afk.yummy.tv.core.designsystem.presenter.components.loader.TvLoadingScreen
import su.afk.yummy.tv.core.designsystem.presenter.dimensions.TvCardSpacing
import su.afk.yummy.tv.core.designsystem.presenter.dimensions.TvScreenPadding
import su.afk.yummy.tv.core.designsystem.presenter.focus.tvFocusRestorer
import su.afk.yummy.tv.core.designsystem.presenter.tv.TvStateMessage
import su.afk.yummy.tv.domain.bloggers.model.BloggerVideoSort
import su.afk.yummy.tv.feature.bloggers.tv.R
import su.afk.yummy.tv.feature.bloggers.view.BloggerVideoTvCard

private const val ALL_CATEGORY_ID = "all"

@Composable
fun BloggerVideosListTvScreen(
    state: BloggerVideosListState.State,
    effect: Flow<BloggerVideosListState.Effect>,
    onEvent: (BloggerVideosListState.Event) -> Unit,
) {
    BackHandler { onEvent(BloggerVideosListState.Event.BackSelected) }
    val error = state.error
    val firstCardFocus = remember { FocusRequester() }
    Column(
        Modifier
            .fillMaxSize()
            .padding(
                horizontal = TvScreenPadding.Horizontal,
                vertical = TvScreenPadding.Vertical,
            ),
        verticalArrangement = Arrangement.spacedBy(TvCardSpacing.Vertical)
    ) {
        Text(
            stringResource(if (state.animeId == null) R.string.blogger_videos_title else R.string.blogger_videos_anime_title),
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onBackground,
        )
        if (state.animeId == null) {
            Row(
                Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(TvCardSpacing.Horizontal)
            ) {
                FilterChip(
                    state.selectedCategory == ALL_CATEGORY_ID,
                    { onEvent(BloggerVideosListState.Event.CategorySelected(ALL_CATEGORY_ID)) },
                    { Text(stringResource(R.string.blogger_videos_all)) })
                state.categories.forEach { category ->
                    FilterChip(
                        state.selectedCategory == category.id,
                        { onEvent(BloggerVideosListState.Event.CategorySelected(category.id)) },
                        { Text(category.title) })
                }
            }
            Row(
                Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(TvCardSpacing.Horizontal)
            ) {
                BloggerVideoSort.entries.forEach { sort ->
                    FilterChip(
                        state.sort == sort,
                        { onEvent(BloggerVideosListState.Event.SortSelected(sort)) },
                        { Text(stringResource(sort.labelRes())) })
                }
            }
            Row(
                Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(TvCardSpacing.Horizontal)
            ) {
                FilterChip(
                    state.selectedBloggerId == null,
                    { onEvent(BloggerVideosListState.Event.BloggerSelected(null)) },
                    { Text(stringResource(R.string.blogger_videos_all_bloggers)) })
                state.bloggers.forEach { blogger ->
                    FilterChip(
                        state.selectedBloggerId == blogger.id,
                        { onEvent(BloggerVideosListState.Event.BloggerSelected(blogger.id)) },
                        { Text(blogger.nickname) })
                }
                state.selectedBloggerId?.let { bloggerId ->
                    FilterChip(
                        false,
                        { onEvent(BloggerVideosListState.Event.BloggerDetailsSelected(bloggerId)) },
                        { Text(stringResource(R.string.blogger_open_page)) },
                    )
                }
            }
        }
        when {
            state.isLoading -> TvLoadingScreen(Modifier.weight(1f))

            error != null -> Box(
                Modifier
                    .fillMaxSize()
                    .weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                TvStateMessage(
                    title = error.ifBlank { stringResource(R.string.blogger_videos_error) },
                    icon = Icons.Filled.Warning,
                    onRetry = { onEvent(BloggerVideosListState.Event.RetrySelected) },
                )
            }

            state.videos.isEmpty() -> Box(
                Modifier
                    .fillMaxSize()
                    .weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                TvStateMessage(
                    title = stringResource(R.string.blogger_videos_empty),
                    icon = Icons.Filled.OndemandVideo,
                )
            }

            else -> {
                LaunchedEffect(state.videos.isNotEmpty()) {
                    runCatching { firstCardFocus.requestFocus() }
                }
                LazyColumn(
                    contentPadding = PaddingValues(
                        top = TvCardSpacing.Vertical,
                        bottom = TvScreenPadding.Vertical,
                    ),
                    verticalArrangement = Arrangement.spacedBy(TvCardSpacing.Vertical),
                    modifier = Modifier
                        .weight(1f)
                        .tvFocusRestorer(fallback = firstCardFocus),
                ) {
                    items(state.videos, key = { it.id }) { video ->
                        BloggerVideoTvCard(
                            video,
                            { onEvent(BloggerVideosListState.Event.VideoSelected(video.id)) },
                            modifier = if (state.videos.firstOrNull()?.id == video.id) {
                                Modifier.focusRequester(firstCardFocus)
                            } else {
                                Modifier
                            },
                        )
                    }
                }
            }
        }
    }
}

private fun BloggerVideoSort.labelRes() = when (this) {
    BloggerVideoSort.NEW -> R.string.blogger_videos_sort_new
    BloggerVideoSort.TOP -> R.string.blogger_videos_sort_top
    BloggerVideoSort.OLD -> R.string.blogger_videos_sort_old
}
