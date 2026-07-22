package su.afk.yummy.tv.feature.details.similar.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
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
    modifier: Modifier = Modifier,
    /** Точка входа фокуса в ряд — кнопка «нравится». */
    focusRequester: FocusRequester? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        VoteButton(
            icon = Icons.Filled.ThumbUp,
            count = item.likes,
            color = LikeColor,
            selected = item.vote == AnimeRecommendationVote.LIKE,
            enabled = enabled,
            contentDescription = stringResource(R.string.details_similar_like),
            onClick = { onVote(AnimeRecommendationVote.LIKE) },
            modifier = focusRequester?.let { Modifier.focusRequester(it) } ?: Modifier,
        )
        VoteButton(
            icon = Icons.Filled.ThumbDown,
            count = item.dislikes,
            color = DislikeColor,
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
    color: Color,
    selected: Boolean,
    enabled: Boolean,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(8.dp)
    // Свой голос — цвет реакции, чужие — тот же цвет, но приглушённый (как на мобилке).
    val tint = when {
        !enabled -> color.copy(alpha = 0.38f)
        selected -> color
        else -> color.copy(alpha = 0.7f)
    }
    Row(
        modifier = modifier
            .clip(shape)
            .tvFocusableClick(
                onClick = { if (enabled) onClick() },
                shape = shape,
                focusedScale = 1f,
            )
            .padding(horizontal = 6.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.padding(2.dp),
        )
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
