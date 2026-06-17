package su.afk.yummy.tv.feature.collection.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import su.afk.yummy.tv.domain.collection.model.CollectionDetail
import su.afk.yummy.tv.domain.collection.model.CollectionVote

@Composable
internal fun CollectionMobileHeader(
    collection: CollectionDetail,
    isVoteLoading: Boolean,
    onVote: (CollectionVote) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = collection.title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
        )
        if (collection.description.isNotBlank()) {
            Text(
                text = collection.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.78f),
                maxLines = 8,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 20.sp,
            )
        }
        CollectionVoteActions(
            collection = collection,
            enabled = !isVoteLoading,
            onVote = onVote,
        )
    }
}

@Composable
private fun CollectionVoteActions(
    collection: CollectionDetail,
    enabled: Boolean,
    onVote: (CollectionVote) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
    ) {
        CollectionVoteButton(
            selected = collection.vote == CollectionVote.LIKE,
            count = collection.likesCount,
            vote = CollectionVote.LIKE,
            enabled = enabled,
            onVote = onVote,
        )
        CollectionVoteButton(
            selected = collection.vote == CollectionVote.DISLIKE,
            count = collection.dislikesCount,
            vote = CollectionVote.DISLIKE,
            enabled = enabled,
            onVote = onVote,
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
) {
    val isLike = vote == CollectionVote.LIKE
    val voteColor = if (isLike) LikeColor else DislikeColor
    val tint = if (selected) voteColor else voteColor.copy(alpha = 0.72f)
    OutlinedButton(
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

private val LikeColor = Color(0xFF69D38B)
private val DislikeColor = Color(0xFFFF6B6B)
