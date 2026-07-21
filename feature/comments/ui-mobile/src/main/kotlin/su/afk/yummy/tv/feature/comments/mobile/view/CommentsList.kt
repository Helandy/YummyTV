package su.afk.yummy.tv.feature.comments.mobile.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.itemKey
import su.afk.yummy.tv.core.designsystem.presenter.mobile.MobileAppendError
import su.afk.yummy.tv.feature.comments.CommentsState
import su.afk.yummy.tv.feature.comments.mobile.R
import su.afk.yummy.tv.feature.comments.mobile.utils.uiMessage

@Composable
internal fun CommentsList(
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
                MobileAppendError(
                    message = error,
                    onRetry = { onEvent(CommentsState.Event.RetrySelected) },
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

internal fun CommentsState.CommentUi.resolve(
    state: CommentsState.State,
): CommentsState.CommentUi? {
    if (comment.id in state.deletedCommentIds) return null
    val overlaid = state.commentOverlays[comment.id] ?: this
    return overlaid.copy(
        children = overlaid.children.mapNotNull { it.resolve(state) },
    )
}
