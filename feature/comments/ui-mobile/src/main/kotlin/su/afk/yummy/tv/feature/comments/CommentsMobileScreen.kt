package su.afk.yummy.tv.feature.comments

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import kotlinx.coroutines.flow.Flow
import su.afk.yummy.tv.core.designsystem.presenter.baseScreen.BaseScreen
import su.afk.yummy.tv.core.designsystem.presenter.mobile.MobileMessage
import su.afk.yummy.tv.core.designsystem.presenter.mobile.MobileStateContent
import su.afk.yummy.tv.core.designsystem.presenter.mobile.MobileTopBar
import su.afk.yummy.tv.domain.comments.model.CommentSort
import su.afk.yummy.tv.feature.comments.mobile.R
import su.afk.yummy.tv.feature.comments.view.CommentThread
import su.afk.yummy.tv.feature.comments.view.CommentsComposer
import su.afk.yummy.tv.feature.comments.view.CommentsDialogs
import su.afk.yummy.tv.feature.comments.view.label

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun CommentsMobileScreen(
    state: CommentsState.State,
    effect: Flow<CommentsState.Effect>,
    onEvent: (CommentsState.Event) -> Unit,
) {
    val context = LocalContext.current
    val pagingComments = state.comments.collectAsLazyPagingItems()
    val refreshState = pagingComments.loadState.refresh
    val appendState = pagingComments.loadState.append
    val visibleComments = remember(
        state.prependedComments,
        state.commentOverlays,
        state.deletedCommentIds,
        pagingComments.itemSnapshotList.items,
    ) {
        (state.prependedComments + pagingComments.itemSnapshotList.items)
            .distinctBy { it.comment.id }
            .mapNotNull { it.resolve(state) }
    }
    val initialError = (refreshState as? LoadState.Error)
        ?.takeIf { visibleComments.isEmpty() }
        ?.error
        ?.uiMessage()
    val listState = rememberLazyListState()
    var sortRowVisible by remember { mutableStateOf(true) }
    var previousScrollIndex by remember { mutableIntStateOf(0) }
    var previousScrollOffset by remember { mutableIntStateOf(0) }

    LaunchedEffect(effect, context) {
        effect.collect { event ->
            when (event) {
                is CommentsState.Effect.ShowToast ->
                    Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
            }
        }
    }
    LaunchedEffect(listState) {
        snapshotFlow {
            listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset
        }.collect { (index, offset) ->
            val scrollingDown = index > previousScrollIndex ||
                    (index == previousScrollIndex && offset > previousScrollOffset)
            val scrollingUp = index < previousScrollIndex ||
                    (index == previousScrollIndex && offset < previousScrollOffset)
            sortRowVisible = when {
                index == 0 && offset == 0 -> true
                scrollingUp -> true
                scrollingDown -> false
                else -> sortRowVisible
            }
            previousScrollIndex = index
            previousScrollOffset = offset
        }
    }
    LaunchedEffect(state.sort) {
        sortRowVisible = true
        listState.scrollToItem(0)
    }
    LaunchedEffect(visibleComments) {
        onEvent(CommentsState.Event.VisibleCommentsChanged(visibleComments))
    }

    BaseScreen(
        isScroll = false,
        customTopBar = {
            MobileTopBar(
                title = stringResource(R.string.comments_title),
                onBack = { onEvent(CommentsState.Event.BackSelected) },
            )
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding(),
        ) {
            AnimatedVisibility(
                visible = sortRowVisible,
                enter = expandVertically(),
                exit = shrinkVertically(),
            ) {
                CommentSortRow(
                    selected = state.sort,
                    onSelected = { onEvent(CommentsState.Event.SortSelected(it)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                )
            }
            Box(Modifier.weight(1f)) {
                PullToRefreshBox(
                    isRefreshing = refreshState is LoadState.Loading && visibleComments.isNotEmpty(),
                    onRefresh = { onEvent(CommentsState.Event.RefreshSelected) },
                    modifier = Modifier.fillMaxSize(),
                ) {
                    MobileStateContent(
                        isLoading = refreshState is LoadState.Loading && visibleComments.isEmpty(),
                        error = initialError,
                        empty = refreshState !is LoadState.Loading &&
                                visibleComments.isEmpty() &&
                                initialError == null,
                        emptyText = stringResource(R.string.comments_empty),
                        onRetry = {
                            onEvent(CommentsState.Event.RetrySelected)
                            pagingComments.retry()
                        },
                    ) {
                        CommentsList(
                            state = state,
                            pagingComments = pagingComments,
                            appendState = appendState,
                            listState = listState,
                            onEvent = onEvent,
                        )
                    }
                }
            }
            CommentsComposer(
                isSignedIn = state.isSignedIn,
                text = state.composerText,
                mode = state.composerMode,
                enabled = !state.isMutating,
                onTextChange = { onEvent(CommentsState.Event.ComposerTextChanged(it)) },
                onSubmit = { onEvent(CommentsState.Event.SubmitSelected) },
                onCancel = { onEvent(CommentsState.Event.ComposerCancelled) },
            )
        }
        CommentsDialogs(
            pendingDelete = state.pendingDelete,
            pendingReport = state.pendingReport,
            isMutating = state.isMutating,
            onDeleteConfirm = { onEvent(CommentsState.Event.DeleteConfirmed) },
            onDeleteDismiss = { onEvent(CommentsState.Event.DeleteDismissed) },
            onReportConfirm = { onEvent(CommentsState.Event.ReportConfirmed(it)) },
            onReportDismiss = { onEvent(CommentsState.Event.ReportDismissed) },
        )
    }
}

@Composable
private fun CommentsList(
    state: CommentsState.State,
    pagingComments: LazyPagingItems<CommentsState.CommentUi>,
    appendState: LoadState,
    listState: LazyListState,
    onEvent: (CommentsState.Event) -> Unit,
) {
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        (appendState as? LoadState.Error)?.error?.uiMessage()?.let { error ->
            item(key = "soft_error") {
                MobileMessage(
                    title = error,
                    actionLabel = stringResource(R.string.comments_retry),
                    onAction = { onEvent(CommentsState.Event.RetrySelected) },
                )
            }
        }
        itemsIndexed(
            items = state.prependedComments,
            key = { _, item -> "prepended_${item.comment.id}" },
        ) { _, item ->
            item.resolve(state)?.let { resolved ->
                CommentThread(
                    item = resolved,
                    currentUserId = state.currentUserId,
                    isModerator = state.isModerator,
                    depth = 0,
                    onReply = { onEvent(CommentsState.Event.ReplySelected(it)) },
                    onEdit = { onEvent(CommentsState.Event.EditSelected(it)) },
                    onDelete = { onEvent(CommentsState.Event.DeleteSelected(it)) },
                    onReport = { onEvent(CommentsState.Event.ReportSelected(it)) },
                    onVote = { commentId, vote ->
                        onEvent(CommentsState.Event.VoteSelected(commentId, vote))
                    },
                    onToggleChildren = {
                        onEvent(CommentsState.Event.ChildrenToggleSelected(it))
                    },
                    onLoadMoreChildren = {
                        onEvent(CommentsState.Event.LoadMoreChildrenSelected(it))
                    },
                    onAuthorSelected = {
                        onEvent(CommentsState.Event.AuthorSelected(it))
                    },
                )
            }
        }
        items(
            count = pagingComments.itemCount,
            key = pagingComments.itemKey { it.comment.id },
        ) { index ->
            val item = pagingComments[index]?.resolve(state) ?: return@items
            if (state.prependedComments.any { it.comment.id == item.comment.id }) return@items
            CommentThread(
                item = item,
                currentUserId = state.currentUserId,
                isModerator = state.isModerator,
                depth = 0,
                onReply = { onEvent(CommentsState.Event.ReplySelected(it)) },
                onEdit = { onEvent(CommentsState.Event.EditSelected(it)) },
                onDelete = { onEvent(CommentsState.Event.DeleteSelected(it)) },
                onReport = { onEvent(CommentsState.Event.ReportSelected(it)) },
                onVote = { commentId, vote ->
                    onEvent(CommentsState.Event.VoteSelected(commentId, vote))
                },
                onToggleChildren = {
                    onEvent(CommentsState.Event.ChildrenToggleSelected(it))
                },
                onLoadMoreChildren = {
                    onEvent(CommentsState.Event.LoadMoreChildrenSelected(it))
                },
                onAuthorSelected = {
                    onEvent(CommentsState.Event.AuthorSelected(it))
                },
            )
        }
        if (appendState is LoadState.Loading) {
            item(key = "loading_more") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.padding(end = 10.dp),
                        strokeWidth = 2.dp,
                    )
                    Text(
                        text = stringResource(R.string.comments_loading),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

private fun CommentsState.CommentUi.resolve(
    state: CommentsState.State,
): CommentsState.CommentUi? {
    if (comment.id in state.deletedCommentIds) return null
    val overlaid = state.commentOverlays[comment.id] ?: this
    return overlaid.copy(
        children = overlaid.children.mapNotNull { it.resolve(state) },
    )
}

private fun Throwable.uiMessage(): String =
    message ?: localizedMessage ?: toString()

@Composable
private fun CommentSortRow(
    selected: CommentSort,
    onSelected: (CommentSort) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
            .padding(2.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        listOf(CommentSort.NEW, CommentSort.BEST, CommentSort.OLD).forEach { sort ->
            val isSelected = sort == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                    .clickable { onSelected(sort) }
                    .padding(vertical = 7.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = sort.label(),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }
        }
    }
}
