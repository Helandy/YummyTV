package su.afk.yummy.tv.feature.comments.mobile.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.domain.comments.model.CommentVote
import su.afk.yummy.tv.feature.comments.CommentsState
import su.afk.yummy.tv.feature.comments.mobile.R

@Composable
internal fun CommentThread(
    item: CommentsState.CommentUi,
    currentUserId: Int,
    isModerator: Boolean,
    depth: Int,
    onReply: (Int) -> Unit,
    onEdit: (Int) -> Unit,
    onDelete: (Int) -> Unit,
    onReport: (Int) -> Unit,
    onVote: (Int, CommentVote) -> Unit,
    onToggleChildren: (Int) -> Unit,
    onLoadMoreChildren: (Int) -> Unit,
    onAuthorSelected: (Int) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        CommentItem(
            comment = item.comment,
            currentUserId = currentUserId,
            isModerator = isModerator,
            childrenVisible = item.childrenVisible,
            onReply = onReply,
            onEdit = onEdit,
            onDelete = onDelete,
            onReport = onReport,
            onVote = onVote,
            onToggleChildren = onToggleChildren,
            onAuthorSelected = onAuthorSelected,
        )
        if (item.childrenVisible) {
            RepliesBranch(depth = depth) {
                item.children.forEach { child ->
                    CommentThread(
                        item = child,
                        currentUserId = currentUserId,
                        isModerator = isModerator,
                        depth = (depth + 1).coerceAtMost(2),
                        onReply = onReply,
                        onEdit = onEdit,
                        onDelete = onDelete,
                        onReport = onReport,
                        onVote = onVote,
                        onToggleChildren = onToggleChildren,
                        onLoadMoreChildren = onLoadMoreChildren,
                        onAuthorSelected = onAuthorSelected,
                    )
                }
                if (item.childrenLoading) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                        Text(
                            text = stringResource(R.string.comments_loading),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                item.childrenError?.let { error ->
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(vertical = 8.dp),
                    )
                }
                if (item.childrenHasMore) {
                    InlineTextAction(
                        text = stringResource(R.string.comments_load_more_replies),
                        onClick = { onLoadMoreChildren(item.comment.id) },
                        modifier = Modifier.padding(vertical = 6.dp),
                    )
                }
            }
        }
    }
}

internal val LikeColor = Color(0xFF69D38B)
internal val DislikeColor = Color(0xFFFF6B6B)
