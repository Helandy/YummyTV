package su.afk.yummy.tv.feature.posts.mobile.list

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import kotlinx.coroutines.flow.Flow
import su.afk.yummy.tv.core.designsystem.presenter.components.StateMessage
import su.afk.yummy.tv.core.designsystem.presenter.mobile.MobileAppendError
import su.afk.yummy.tv.core.designsystem.presenter.mobile.MobileSectionLoading
import su.afk.yummy.tv.domain.posts.model.PostSort
import su.afk.yummy.tv.feature.posts.list.PostsListState
import su.afk.yummy.tv.feature.posts.mobile.R
import su.afk.yummy.tv.feature.posts.mobile.view.PostMobileCard
import su.afk.yummy.tv.feature.posts.mobile.view.PostsLoadingState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostsMobileScreen(
    state: PostsListState.State,
    effect: Flow<PostsListState.Effect>,
    onEvent: (PostsListState.Event) -> Unit,
) {
    val posts = state.posts.collectAsLazyPagingItems()
    val categoryChipColors = FilterChipDefaults.filterChipColors(
        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.34f),
        labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
        selectedContainerColor = MaterialTheme.colorScheme.primary,
        selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
    )
    val sortTabColors = SegmentedButtonDefaults.colors(
        activeContainerColor = MaterialTheme.colorScheme.primary,
        activeContentColor = MaterialTheme.colorScheme.onPrimary,
        activeBorderColor = MaterialTheme.colorScheme.primary,
        inactiveContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.34f),
        inactiveContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        inactiveBorderColor = MaterialTheme.colorScheme.outlineVariant,
    )
    Scaffold(topBar = { TopAppBar(title = { Text(stringResource(R.string.posts_title)) }) }) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    PostSort.entries.forEachIndexed { index, sort ->
                        SegmentedButton(
                            selected = state.sort == sort,
                            onClick = { onEvent(PostsListState.Event.SortSelected(sort)) },
                            shape = SegmentedButtonDefaults.itemShape(index, PostSort.entries.size),
                            colors = sortTabColors,
                        ) { Text(sort.label()) }
                    }
                }
            }
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item(key = "all") {
                        FilterChip(
                            selected = state.selectedCategory == null,
                            onClick = { onEvent(PostsListState.Event.CategorySelected(null)) },
                            label = { Text(stringResource(R.string.posts_all)) },
                            shape = RoundedCornerShape(6.dp),
                            colors = categoryChipColors,
                        )
                    }
                    items(state.categories, key = { it.uri }) { category ->
                        FilterChip(
                            selected = state.selectedCategory == category.uri,
                            onClick = { onEvent(PostsListState.Event.CategorySelected(category.uri)) },
                            label = { Text(category.title) },
                            shape = RoundedCornerShape(6.dp),
                            colors = categoryChipColors,
                        )
                    }
                }
            }
            when {
                posts.loadState.refresh is LoadState.Loading -> item {
                    PostsLoadingState()
                }

                posts.loadState.refresh is LoadState.Error -> item {
                    StateMessage(
                        stringResource(R.string.posts_error),
                        fillMaxSize = false,
                        actionLabel = stringResource(R.string.posts_retry),
                        onAction = posts::retry
                    )
                }

                posts.itemCount == 0 -> item {
                    StateMessage(
                        stringResource(R.string.posts_empty),
                        fillMaxSize = false
                    )
                }

                else -> items(posts.itemCount, key = posts.itemKey { it.id }) { index ->
                    posts[index]?.let { post ->
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
                    MobileAppendError(
                        message = stringResource(R.string.posts_error),
                        onRetry = posts::retry,
                    )
                }
            }
        }
    }
}

@Composable
private fun PostSort.label() = when (this) {
    PostSort.NEW -> stringResource(R.string.posts_new)
    PostSort.OLD -> stringResource(R.string.posts_old)
    PostSort.BEST -> stringResource(R.string.posts_best)
}
