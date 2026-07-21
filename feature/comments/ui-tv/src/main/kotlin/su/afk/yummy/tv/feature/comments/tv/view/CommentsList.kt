package su.afk.yummy.tv.feature.comments.tv.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.itemKey
import su.afk.yummy.tv.core.designsystem.presenter.components.loader.TvLoadingFooter
import su.afk.yummy.tv.core.designsystem.presenter.focus.tvFocusRestorer
import su.afk.yummy.tv.core.designsystem.presenter.tv.TvAppendErrorFooter
import su.afk.yummy.tv.domain.comments.model.CommentVote
import su.afk.yummy.tv.feature.comments.CommentsState
import su.afk.yummy.tv.feature.comments.tv.utils.resolve
import su.afk.yummy.tv.feature.comments.tv.utils.uiMessage

@Composable
internal fun CommentsList(
    state: CommentsState.State,
    comments: LazyPagingItems<CommentsState.CommentUi>,
    listState: LazyListState,
    fallbackFocusRequester: FocusRequester,
    onReply: (Int) -> Unit,
    onEdit: (Int) -> Unit,
    onDelete: (Int, FocusRequester) -> Unit,
    onReport: (Int, FocusRequester) -> Unit,
    onVote: (Int, CommentVote) -> Unit,
    onToggleChildren: (Int) -> Unit,
    onLoadMoreChildren: (Int) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        state = listState,
        modifier = modifier.tvFocusRestorer(fallback = fallbackFocusRequester),
        contentPadding = PaddingValues(bottom = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        itemsIndexed(
            items = state.prependedComments,
            key = { _, item -> "prepended_${item.comment.id}" },
        ) { _, item ->
            item.resolve(state)?.let { resolved ->
                CommentCard(
                    item = resolved,
                    currentUserId = state.currentUserId,
                    isModerator = state.isModerator,
                    isMutating = state.isMutating,
                    depth = 0,
                    onReply = onReply,
                    onEdit = onEdit,
                    onDelete = onDelete,
                    onReport = onReport,
                    onVote = onVote,
                    onToggleChildren = onToggleChildren,
                    onLoadMoreChildren = onLoadMoreChildren,
                )
            }
        }
        items(
            count = comments.itemCount,
            key = comments.itemKey { it.comment.id },
        ) { index ->
            val item = comments[index]?.resolve(state) ?: return@items
            if (state.prependedComments.any { it.comment.id == item.comment.id }) return@items
            CommentCard(
                item = item,
                currentUserId = state.currentUserId,
                isModerator = state.isModerator,
                isMutating = state.isMutating,
                depth = 0,
                onReply = onReply,
                onEdit = onEdit,
                onDelete = onDelete,
                onReport = onReport,
                onVote = onVote,
                onToggleChildren = onToggleChildren,
                onLoadMoreChildren = onLoadMoreChildren,
            )
        }
        when (val append = comments.loadState.append) {
            is LoadState.Loading -> item(key = "append_loading") {
                TvLoadingFooter()
            }

            is LoadState.Error -> item(key = "append_error") {
                TvAppendErrorFooter(
                    message = append.error.uiMessage(),
                    onRetry = onRetry,
                )
            }

            else -> Unit
        }
    }
}
