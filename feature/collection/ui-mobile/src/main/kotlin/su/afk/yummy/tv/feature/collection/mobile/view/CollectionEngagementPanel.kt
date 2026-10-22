package su.afk.yummy.tv.feature.collection.mobile.view

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.core.designsystem.mobile.MobileReactionSelection
import su.afk.yummy.tv.core.designsystem.mobile.MobileReactionsRow
import su.afk.yummy.tv.domain.collection.model.CollectionDetail
import su.afk.yummy.tv.domain.collection.model.CollectionVote

@Composable
internal fun CollectionEngagementPanel(
    collection: CollectionDetail,
    voting: Boolean,
    onVote: (CollectionVote) -> Unit,
    onCommentsClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    MobileReactionsRow(
        likes = collection.likesCount,
        dislikes = collection.dislikesCount,
        selection = when (collection.vote) {
            CollectionVote.LIKE -> MobileReactionSelection.LIKE
            CollectionVote.DISLIKE -> MobileReactionSelection.DISLIKE
            else -> MobileReactionSelection.NONE
        },
        enabled = !voting,
        onLikeClick = { onVote(CollectionVote.LIKE) },
        onDislikeClick = { onVote(CollectionVote.DISLIKE) },
        modifier = modifier,
    ) {
        CommentsButton(
            commentsCount = collection.commentsCount,
            onClick = onCommentsClick,
        )
    }
}

/** На узком экране подпись «Комментарии» не помещается — оставляем иконку и число. */
private val CommentsLabelMinWidth = 150.dp

@Composable
private fun RowScope.CommentsButton(
    commentsCount: Int,
    onClick: () -> Unit,
) {
    val label = stringResource(su.afk.yummy.tv.feature.collection.presentation.R.string.collection_comments)
    BoxWithConstraints(modifier = Modifier.weight(1f)) {
        val showLabel = maxWidth >= CommentsLabelMinWidth
        FilledTonalButton(
            onClick = onClick,
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
        ) {
            Icon(Icons.Filled.ChatBubbleOutline, contentDescription = label)
            Text(
                text = when {
                    showLabel && commentsCount > 0 -> "$label · $commentsCount"
                    showLabel -> label
                    else -> commentsCount.toString()
                },
                modifier = Modifier.padding(start = 8.dp),
                maxLines = 1,
            )
        }
    }
}
