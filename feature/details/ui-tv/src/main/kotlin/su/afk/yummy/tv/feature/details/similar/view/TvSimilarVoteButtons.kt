package su.afk.yummy.tv.feature.details.similar.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.core.designsystem.presenter.focus.tvFocusableClick
import su.afk.yummy.tv.core.model.anime.AnimeRecommendation
import su.afk.yummy.tv.core.model.anime.AnimeRecommendationVote
import su.afk.yummy.tv.feature.details.R

@Composable
internal fun TvSimilarVoteButtons(
    item: AnimeRecommendation,
    enabled: Boolean,
    onVote: (AnimeRecommendationVote) -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        VoteButton(
            icon = Icons.Filled.ThumbUp,
            count = item.likes,
            selected = item.vote == AnimeRecommendationVote.LIKE,
            enabled = enabled,
            contentDescription = stringResource(R.string.details_similar_like),
            onClick = { onVote(AnimeRecommendationVote.LIKE) },
        )
        VoteButton(
            icon = Icons.Filled.ThumbDown,
            count = item.dislikes,
            selected = item.vote == AnimeRecommendationVote.DISLIKE,
            enabled = enabled,
            contentDescription = stringResource(R.string.details_similar_dislike),
            onClick = { onVote(AnimeRecommendationVote.DISLIKE) },
        )
    }
}

@Composable
private fun VoteButton(
    icon: ImageVector,
    count: Int,
    selected: Boolean,
    enabled: Boolean,
    contentDescription: String,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(8.dp)
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = when {
                !enabled -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                selected -> MaterialTheme.colorScheme.primary
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            },
            modifier = Modifier
                .clip(shape)
                .tvFocusableClick(onClick = { if (enabled) onClick() }, shape = shape)
                .padding(8.dp),
        )
        Text(text = count.toString(), style = MaterialTheme.typography.labelLarge)
    }
}
