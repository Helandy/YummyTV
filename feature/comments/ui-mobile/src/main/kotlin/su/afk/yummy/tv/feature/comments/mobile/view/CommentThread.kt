package su.afk.yummy.tv.feature.comments.mobile.view

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import su.afk.yummy.tv.domain.comments.model.Comment
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

@Composable
private fun RepliesBranch(
    depth: Int,
    content: @Composable () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = if (depth == 0) 16.dp else 12.dp)
            .height(IntrinsicSize.Min),
    ) {
        Box(
            modifier = Modifier
                .width(1.dp)
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.72f)),
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 10.dp),
        ) {
            content()
        }
    }
}

@Composable
private fun CommentItem(
    comment: Comment,
    currentUserId: Int,
    isModerator: Boolean,
    childrenVisible: Boolean,
    onReply: (Int) -> Unit,
    onEdit: (Int) -> Unit,
    onDelete: (Int) -> Unit,
    onReport: (Int) -> Unit,
    onVote: (Int, CommentVote) -> Unit,
    onToggleChildren: (Int) -> Unit,
    onAuthorSelected: (Int) -> Unit,
) {
    val canEdit = currentUserId == comment.author.id || isModerator
    Column(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp),
            verticalAlignment = Alignment.Top,
        ) {
            AsyncImage(
                model = comment.author.avatarSmallUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .clickable(enabled = comment.author.id > 0) {
                        onAuthorSelected(comment.author.id)
                    }
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)),
            )
            Spacer(Modifier.width(10.dp))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = comment.author.name.ifBlank { stringResource(R.string.comments_unknown_author) },
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(enabled = comment.author.id > 0) {
                                onAuthorSelected(comment.author.id)
                            }
                            .padding(horizontal = 2.dp, vertical = 1.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = comment.createdAtEpochSeconds.formatCommentDate(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.68f),
                    )
                    Spacer(Modifier.width(2.dp))
                    CommentMenu(
                        canEdit = canEdit,
                        onReply = { onReply(comment.id) },
                        onEdit = { onEdit(comment.id) },
                        onDelete = { onDelete(comment.id) },
                        onReport = { onReport(comment.id) },
                    )
                }
                if (comment.deletedAtEpochSeconds != null) {
                    Text(
                        text = stringResource(R.string.comments_deleted),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f),
                    )
                } else {
                    CommentBodyText(text = comment.text)
                }
                CommentActionsRow(
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    VoteButton(
                        selected = comment.vote == CommentVote.LIKE,
                        count = comment.likes,
                        isLike = true,
                        onClick = { onVote(comment.id, CommentVote.LIKE) },
                    )
                    VoteButton(
                        selected = comment.vote == CommentVote.DISLIKE,
                        count = comment.dislikes,
                        isLike = false,
                        onClick = { onVote(comment.id, CommentVote.DISLIKE) },
                    )
                    InlineTextAction(
                        text = stringResource(R.string.comments_reply),
                        icon = Icons.AutoMirrored.Filled.Reply,
                        onClick = { onReply(comment.id) },
                    )
                    if (comment.childrenCount > 0) {
                        InlineTextAction(
                            text = if (childrenVisible) {
                                stringResource(R.string.comments_hide_replies)
                            } else {
                                stringResource(
                                    R.string.comments_show_replies,
                                    comment.childrenCount
                                )
                            },
                            icon = if (childrenVisible) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                            onClick = { onToggleChildren(comment.id) },
                        )
                    }
                }
            }
        }
        HorizontalDivider(
            thickness = 0.5.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.58f),
        )
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun CommentActionsRow(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        content()
    }
}

@Composable
private fun VoteButton(
    selected: Boolean,
    count: Int,
    isLike: Boolean,
    onClick: () -> Unit,
) {
    val voteColor = if (isLike) LikeColor else DislikeColor
    val tint = if (selected) voteColor else voteColor.copy(alpha = 0.72f)
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(
            imageVector = if (isLike) Icons.Filled.ThumbUp else Icons.Filled.ThumbDown,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(16.dp),
        )
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            color = tint,
        )
    }
}

private val LikeColor = Color(0xFF69D38B)
private val DislikeColor = Color(0xFFFF6B6B)

@Composable
private fun InlineTextAction(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp),
            )
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun CommentMenu(
    canEdit: Boolean,
    onReply: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onReport: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(
            onClick = { expanded = true },
            modifier = Modifier.size(32.dp),
        ) {
            Icon(
                imageVector = Icons.Filled.MoreVert,
                contentDescription = stringResource(R.string.comments_actions),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp),
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.comments_reply)) },
                leadingIcon = { Icon(Icons.AutoMirrored.Filled.Reply, contentDescription = null) },
                onClick = {
                    expanded = false
                    onReply()
                },
            )
            if (canEdit) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.comments_edit)) },
                    leadingIcon = { Icon(Icons.Filled.Edit, contentDescription = null) },
                    onClick = {
                        expanded = false
                        onEdit()
                    },
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.comments_delete)) },
                    leadingIcon = { Icon(Icons.Filled.Delete, contentDescription = null) },
                    onClick = {
                        expanded = false
                        onDelete()
                    },
                )
            }
            DropdownMenuItem(
                text = { Text(stringResource(R.string.comments_report)) },
                leadingIcon = { Icon(Icons.Filled.Flag, contentDescription = null) },
                onClick = {
                    expanded = false
                    onReport()
                },
            )
        }
    }
}
