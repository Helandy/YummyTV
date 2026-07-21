package su.afk.yummy.tv.feature.details.mobile.similar.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import su.afk.yummy.tv.core.model.anime.AnimeRecommendation
import su.afk.yummy.tv.core.model.anime.AnimeRecommendationVote
import su.afk.yummy.tv.feature.details.mobile.R

@Composable
internal fun MobileSimilarVoteButtons(
    item: AnimeRecommendation,
    enabled: Boolean,
    onVote: (AnimeRecommendationVote) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        VoteButton(
            count = item.likes,
            selected = item.vote == AnimeRecommendationVote.LIKE,
            enabled = enabled,
            contentDescription = stringResource(R.string.details_mobile_similar_like),
            icon = Icons.Filled.ThumbUp,
            onClick = { onVote(AnimeRecommendationVote.LIKE) },
        )
        VoteButton(
            count = item.dislikes,
            selected = item.vote == AnimeRecommendationVote.DISLIKE,
            enabled = enabled,
            contentDescription = stringResource(R.string.details_mobile_similar_dislike),
            icon = Icons.Filled.ThumbDown,
            onClick = { onVote(AnimeRecommendationVote.DISLIKE) },
        )
    }
}

@Composable
private fun VoteButton(
    count: Int,
    selected: Boolean,
    enabled: Boolean,
    contentDescription: String,
    icon: ImageVector,
    onClick: () -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onClick, enabled = enabled) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = if (selected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(text = count.toString(), style = MaterialTheme.typography.labelMedium)
    }
}
