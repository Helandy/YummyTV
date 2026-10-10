package su.afk.yummy.tv.feature.collection.mobile.view

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
import su.afk.yummy.tv.core.designsystem.mobile.MobileReactionSelection
import su.afk.yummy.tv.core.designsystem.mobile.MobileReactionsCard
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
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        MobileReactionsCard(
            title = stringResource(su.afk.yummy.tv.feature.collection.presentation.R.string.collection_reactions_title),
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
        )
        FilledTonalButton(
            onClick = onCommentsClick,
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(vertical = 13.dp),
        ) {
            Icon(Icons.Filled.ChatBubbleOutline, contentDescription = null)
            Text(
                text = stringResource(su.afk.yummy.tv.feature.collection.presentation.R.string.collection_comments),
                modifier = Modifier.padding(start = 8.dp),
            )
        }
    }
}
