package su.afk.yummy.tv.feature.comments.tv.view

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import su.afk.yummy.tv.domain.comments.model.CommentVote
import su.afk.yummy.tv.feature.comments.CommentsState
import su.afk.yummy.tv.feature.comments.tv.R
import su.afk.yummy.tv.feature.comments.tv.utils.formatCommentDate

@Composable
@OptIn(ExperimentalLayoutApi::class)
internal fun CommentCard(
    item: CommentsState.CommentUi,
    currentUserId: Int,
    isModerator: Boolean,
    isMutating: Boolean,
    depth: Int,
    onReply: (Int) -> Unit,
    onEdit: (Int) -> Unit,
    onDelete: (Int, FocusRequester) -> Unit,
    onReport: (Int, FocusRequester) -> Unit,
    onVote: (Int, CommentVote) -> Unit,
    onToggleChildren: (Int) -> Unit,
    onLoadMoreChildren: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val comment = item.comment
    val canEdit = currentUserId == comment.author.id || isModerator
    val deleteFocusRequester = androidx.compose.runtime.remember { FocusRequester() }
    val reportFocusRequester = androidx.compose.runtime.remember { FocusRequester() }
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.34f),
        shape = RoundedCornerShape(14.dp),
        tonalElevation = 1.dp,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AsyncImage(
                    model = comment.author.avatarSmallUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(38.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = comment.author.name.ifBlank {
                        stringResource(R.string.comments_unknown_author)
                    },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = comment.createdAtEpochSeconds.formatCommentDate(),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (comment.deletedAtEpochSeconds != null) {
                Text(
                    text = stringResource(R.string.comments_deleted),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyLarge,
                )
            } else {
                CommentBodyText(comment.text)
            }

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (comment.deletedAtEpochSeconds == null) {
                    CommentActionButton(
                        label = comment.likes.toString(),
                        icon = Icons.Filled.ThumbUp,
                        selected = comment.vote == CommentVote.LIKE,
                        enabled = !isMutating,
                        onClick = { onVote(comment.id, CommentVote.LIKE) },
                    )
                    CommentActionButton(
                        label = comment.dislikes.toString(),
                        icon = Icons.Filled.ThumbDown,
                        selected = comment.vote == CommentVote.DISLIKE,
                        selectedColor = MaterialTheme.colorScheme.error,
                        enabled = !isMutating,
                        onClick = { onVote(comment.id, CommentVote.DISLIKE) },
                    )
                    CommentActionButton(
                        label = stringResource(R.string.comments_reply),
                        icon = Icons.AutoMirrored.Filled.Reply,
                        enabled = !isMutating,
                        onClick = { onReply(comment.id) },
                    )
                    if (canEdit) {
                        CommentActionButton(
                            label = stringResource(R.string.comments_edit),
                            icon = Icons.Filled.Edit,
                            enabled = !isMutating,
                            onClick = { onEdit(comment.id) },
                        )
                        CommentActionButton(
                            label = stringResource(R.string.comments_delete),
                            icon = Icons.Filled.Delete,
                            enabled = !isMutating,
                            modifier = Modifier.focusRequester(deleteFocusRequester),
                            onClick = { onDelete(comment.id, deleteFocusRequester) },
                        )
                    }
                    CommentActionButton(
                        label = stringResource(R.string.comments_report),
                        icon = Icons.Filled.Flag,
                        enabled = !isMutating,
                        modifier = Modifier.focusRequester(reportFocusRequester),
                        onClick = { onReport(comment.id, reportFocusRequester) },
                    )
                }
                if (comment.childrenCount > 0 || item.children.isNotEmpty()) {
                    CommentActionButton(
                        label = if (item.childrenVisible) {
                            stringResource(R.string.comments_hide_replies)
                        } else {
                            stringResource(R.string.comments_show_replies, comment.childrenCount)
                        },
                        icon = if (item.childrenVisible) {
                            Icons.Filled.ExpandLess
                        } else {
                            Icons.Filled.ExpandMore
                        },
                        enabled = !item.childrenLoading,
                        onClick = { onToggleChildren(comment.id) },
                    )
                }
            }

            if (item.childrenVisible) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = if (depth == 0) 24.dp else 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    item.children.forEach { child ->
                        CommentCard(
                            item = child,
                            currentUserId = currentUserId,
                            isModerator = isModerator,
                            isMutating = isMutating,
                            depth = (depth + 1).coerceAtMost(2),
                            onReply = onReply,
                            onEdit = onEdit,
                            onDelete = onDelete,
                            onReport = onReport,
                            onVote = onVote,
                            onToggleChildren = onToggleChildren,
                            onLoadMoreChildren = onLoadMoreChildren,
                        )
                    }
                    if (item.childrenLoading) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                            Text(stringResource(R.string.comments_loading))
                        }
                    }
                    item.childrenError?.let { error ->
                        Text(
                            text = error,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                    if (item.childrenHasMore && !item.childrenLoading) {
                        Box {
                            CommentActionButton(
                                label = stringResource(R.string.comments_load_more_replies),
                                icon = Icons.Filled.ExpandMore,
                                onClick = { onLoadMoreChildren(comment.id) },
                            )
                        }
                    }
                }
            }
        }
    }
}
