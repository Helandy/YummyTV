package su.afk.yummy.tv.feature.collection.view

import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.core.designsystem.presenter.theme.YummySemanticColors
import su.afk.yummy.tv.domain.collection.model.CollectionDetail
import su.afk.yummy.tv.domain.collection.model.CollectionVote

@Composable
internal fun CollectionHeader(
    collection: CollectionDetail,
    isVoteLoading: Boolean,
    isOwner: Boolean,
    isMutationLoading: Boolean,
    onVote: (CollectionVote) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onComments: () -> Unit,
    titleFocusRequester: FocusRequester? = null,
    downFocusRequester: FocusRequester? = null,
    modifier: Modifier = Modifier,
) {
    val titleInteractionSource = remember { MutableInteractionSource() }
    val isTitleFocused by titleInteractionSource.collectIsFocusedAsState()
    val likeFocusRequester = remember { FocusRequester() }
    val dislikeFocusRequester = remember { FocusRequester() }
    val editFocusRequester = remember { FocusRequester() }
    val deleteFocusRequester = remember { FocusRequester() }

    Row(
        modifier = modifier
            .padding(bottom = 8.dp)
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .then(
                    if (titleFocusRequester != null) {
                        Modifier.focusRequester(titleFocusRequester)
                    } else {
                        Modifier
                    },
                )
                .focusProperties {
                    right = if (isOwner) editFocusRequester else likeFocusRequester
                    if (downFocusRequester != null) {
                        down = downFocusRequester
                    }
                }
                .border(
                    width = 2.dp,
                    color = if (isTitleFocused) MaterialTheme.colorScheme.primary else Color.Transparent,
                    shape = RoundedCornerShape(8.dp),
                )
                .padding(horizontal = 8.dp, vertical = 6.dp)
                .focusable(interactionSource = titleInteractionSource),
        ) {
            Text(
                text = collection.title,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = if (isTitleFocused) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onBackground
                },
            )

            if (collection.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = collection.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        if (isOwner) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onEdit,
                    enabled = !isMutationLoading,
                    modifier = Modifier
                        .focusRequester(editFocusRequester)
                        .focusProperties {
                            if (titleFocusRequester != null) left = titleFocusRequester
                            down = deleteFocusRequester
                        },
                ) {
                    Text(stringResource(su.afk.yummy.tv.feature.collection.R.string.collection_edit))
                }
                OutlinedButton(
                    onClick = onDelete,
                    enabled = !isMutationLoading,
                    modifier = Modifier
                        .focusRequester(deleteFocusRequester)
                        .focusProperties {
                            if (titleFocusRequester != null) left = titleFocusRequester
                            up = editFocusRequester
                            down = likeFocusRequester
                        },
                ) {
                    Text(stringResource(su.afk.yummy.tv.feature.collection.R.string.collection_delete))
                }
            }
        }

        CollectionVoteActions(
            collection = collection,
            enabled = !isVoteLoading,
            onVote = onVote,
            titleFocusRequester = titleFocusRequester,
            likeFocusRequester = likeFocusRequester,
            dislikeFocusRequester = dislikeFocusRequester,
            leftFocusRequester = if (isOwner) deleteFocusRequester else titleFocusRequester,
            downFocusRequester = downFocusRequester,
        )
        OutlinedButton(onClick = onComments) {
            Text(stringResource(su.afk.yummy.tv.feature.collection.R.string.collection_comments))
        }
    }
}

@Composable
private fun CollectionVoteActions(
    collection: CollectionDetail,
    enabled: Boolean,
    onVote: (CollectionVote) -> Unit,
    titleFocusRequester: FocusRequester?,
    likeFocusRequester: FocusRequester,
    dislikeFocusRequester: FocusRequester,
    leftFocusRequester: FocusRequester?,
    downFocusRequester: FocusRequester?,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.End,
    ) {
        CollectionVoteButton(
            selected = collection.vote == CollectionVote.LIKE,
            count = collection.likesCount,
            vote = CollectionVote.LIKE,
            enabled = enabled,
            onVote = onVote,
            modifier = Modifier
                .focusRequester(likeFocusRequester)
                .collectionVoteFocus(
                    leftFocusRequester = leftFocusRequester,
                    downFocusRequester = dislikeFocusRequester,
                ),
        )
        CollectionVoteButton(
            selected = collection.vote == CollectionVote.DISLIKE,
            count = collection.dislikesCount,
            vote = CollectionVote.DISLIKE,
            enabled = enabled,
            onVote = onVote,
            modifier = Modifier
                .focusRequester(dislikeFocusRequester)
                .collectionVoteFocus(
                    leftFocusRequester = leftFocusRequester,
                    downFocusRequester = downFocusRequester,
                ),
        )
    }
}

@Composable
private fun CollectionVoteButton(
    selected: Boolean,
    count: Int,
    vote: CollectionVote,
    enabled: Boolean,
    onVote: (CollectionVote) -> Unit,
    modifier: Modifier = Modifier,
) {
    val isLike = vote == CollectionVote.LIKE
    val voteColor = if (isLike) YummySemanticColors.Like else YummySemanticColors.Dislike
    val tint = if (selected) voteColor else voteColor.copy(alpha = 0.72f)
    OutlinedButton(
        modifier = modifier.width(112.dp),
        enabled = enabled,
        onClick = { onVote(vote) },
    ) {
        Icon(
            imageVector = if (isLike) Icons.Filled.ThumbUp else Icons.Filled.ThumbDown,
            contentDescription = null,
            tint = tint,
            modifier = Modifier
                .size(18.dp)
                .padding(end = 4.dp),
        )
        Text(
            text = count.toString(),
            color = tint,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
        )
    }
}

private fun Modifier.collectionVoteFocus(
    leftFocusRequester: FocusRequester?,
    downFocusRequester: FocusRequester?,
): Modifier =
    focusProperties {
        if (leftFocusRequester != null) {
            left = leftFocusRequester
        }
        if (downFocusRequester != null) {
            down = downFocusRequester
        }
    }
