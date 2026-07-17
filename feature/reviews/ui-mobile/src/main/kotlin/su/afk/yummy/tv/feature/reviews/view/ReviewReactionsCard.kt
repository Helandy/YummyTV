package su.afk.yummy.tv.feature.reviews.view

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import su.afk.yummy.tv.core.designsystem.presenter.mobile.MobileReactionSelection
import su.afk.yummy.tv.core.designsystem.presenter.mobile.MobileReactionsCard
import su.afk.yummy.tv.domain.reviews.model.ReviewReactions
import su.afk.yummy.tv.domain.reviews.model.ReviewVote
import su.afk.yummy.tv.feature.reviews.mobile.R

@Composable
internal fun ReviewReactionsCard(
    reactions: ReviewReactions,
    onVote: (ReviewVote) -> Unit,
    modifier: Modifier = Modifier,
) {
    MobileReactionsCard(
        title = stringResource(R.string.review_reactions_title),
        likes = reactions.likes,
        dislikes = reactions.dislikes,
        selection = when (reactions.vote) {
            ReviewVote.LIKE -> MobileReactionSelection.LIKE
            ReviewVote.DISLIKE -> MobileReactionSelection.DISLIKE
            ReviewVote.NONE -> MobileReactionSelection.NONE
        },
        enabled = true,
        onLikeClick = {
            onVote(if (reactions.vote == ReviewVote.LIKE) ReviewVote.NONE else ReviewVote.LIKE)
        },
        onDislikeClick = {
            onVote(
                if (reactions.vote == ReviewVote.DISLIKE) ReviewVote.NONE else ReviewVote.DISLIKE
            )
        },
        modifier = modifier,
    )
}
