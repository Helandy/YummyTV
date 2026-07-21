package su.afk.yummy.tv.feature.comments.mobile.view

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import su.afk.yummy.tv.domain.comments.model.Comment
import su.afk.yummy.tv.domain.comments.model.CommentVote
import su.afk.yummy.tv.feature.comments.mobile.R
import su.afk.yummy.tv.feature.comments.mobile.utils.formatCommentDate

@Composable
internal fun CommentItem(
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
