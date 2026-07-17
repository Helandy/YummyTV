package su.afk.yummy.tv.feature.posts.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import su.afk.yummy.tv.core.designsystem.presenter.mobile.MobileReactionSelection
import su.afk.yummy.tv.core.designsystem.presenter.mobile.MobileReactionsCard
import su.afk.yummy.tv.core.utils.toCompactCount
import su.afk.yummy.tv.domain.posts.model.PostReaction
import su.afk.yummy.tv.domain.posts.model.PostVote
import su.afk.yummy.tv.feature.posts.mobile.R

@Composable
internal fun PostEngagementPanel(
    reaction: PostReaction,
    comments: Int,
    voting: Boolean,
    onVote: (PostVote) -> Unit,
    onCommentsClick: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        MobileReactionsCard(
            title = stringResource(R.string.posts_reactions_title),
            likes = reaction.likes,
            dislikes = reaction.dislikes,
            selection = when (reaction.vote) {
                PostVote.LIKE -> MobileReactionSelection.LIKE
                PostVote.DISLIKE -> MobileReactionSelection.DISLIKE
                PostVote.NONE -> MobileReactionSelection.NONE
            },
            enabled = !voting,
            onLikeClick = { onVote(PostVote.LIKE) },
            onDislikeClick = { onVote(PostVote.DISLIKE) },
        )
        FilledTonalButton(
            onClick = onCommentsClick,
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(vertical = 13.dp),
        ) {
            Icon(Icons.Filled.ChatBubbleOutline, contentDescription = null)
            Text(
                text = stringResource(R.string.posts_comments, comments.toCompactCount()),
                modifier = Modifier.padding(start = 8.dp),
            )
        }
    }
}
