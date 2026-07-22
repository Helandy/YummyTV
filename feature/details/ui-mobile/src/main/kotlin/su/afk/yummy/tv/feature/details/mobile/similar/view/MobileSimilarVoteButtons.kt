package su.afk.yummy.tv.feature.details.mobile.similar.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        VoteButton(
            count = item.likes,
            color = LikeColor,
            selected = item.vote == AnimeRecommendationVote.LIKE,
            enabled = enabled,
            contentDescription = stringResource(R.string.details_mobile_similar_like),
            icon = Icons.Filled.ThumbUp,
            onClick = { onVote(AnimeRecommendationVote.LIKE) },
        )
        VoteButton(
            count = item.dislikes,
            color = DislikeColor,
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
    color: Color,
    selected: Boolean,
    enabled: Boolean,
    contentDescription: String,
    icon: ImageVector,
    onClick: () -> Unit,
) {
    // Свой голос — залитая кнопка в цвет реакции, чужие — тот же цвет, но приглушённый.
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(
            onClick = onClick,
            enabled = enabled,
            colors = IconButtonDefaults.iconButtonColors(
                containerColor = if (selected) color.copy(alpha = 0.18f) else Color.Transparent,
                contentColor = if (selected) color else color.copy(alpha = 0.7f),
            ),
        ) {
            Icon(imageVector = icon, contentDescription = contentDescription)
        }
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = if (selected) color else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private val LikeColor = Color(0xFF69F0AE)
private val DislikeColor = Color(0xFFE53935)
