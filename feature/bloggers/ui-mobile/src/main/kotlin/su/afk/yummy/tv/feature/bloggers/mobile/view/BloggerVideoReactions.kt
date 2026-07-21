package su.afk.yummy.tv.feature.bloggers.mobile.view

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import su.afk.yummy.tv.core.designsystem.presenter.mobile.MobileReactionSelection
import su.afk.yummy.tv.core.designsystem.presenter.mobile.MobileReactionsCard
import su.afk.yummy.tv.domain.bloggers.model.BloggerVideoReaction
import su.afk.yummy.tv.domain.bloggers.model.BloggerVideoVote
import su.afk.yummy.tv.feature.bloggers.mobile.R

@Composable
internal fun BloggerVideoReactions(
    reaction: BloggerVideoReaction,
    enabled: Boolean,
    onVote: (BloggerVideoVote) -> Unit,
    modifier: Modifier = Modifier,
) {
    MobileReactionsCard(
        title = stringResource(R.string.blogger_video_reactions_title),
        likes = reaction.likes,
        dislikes = reaction.dislikes,
        selection = when (reaction.vote) {
            BloggerVideoVote.LIKE -> MobileReactionSelection.LIKE
            BloggerVideoVote.DISLIKE -> MobileReactionSelection.DISLIKE
            BloggerVideoVote.NONE -> MobileReactionSelection.NONE
        },
        enabled = enabled,
        onLikeClick = { onVote(BloggerVideoVote.LIKE) },
        onDislikeClick = { onVote(BloggerVideoVote.DISLIKE) },
        modifier = modifier,
    )
}
