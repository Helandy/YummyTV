package su.afk.yummy.tv.feature.reviews.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import su.afk.yummy.tv.core.designsystem.presenter.focus.tvFocusableClick
import su.afk.yummy.tv.core.designsystem.presenter.theme.YummySemanticColors
import su.afk.yummy.tv.core.utils.htmlToPlainText
import su.afk.yummy.tv.core.utils.toCompactCount
import su.afk.yummy.tv.domain.reviews.model.AnimeReviewSummary
import su.afk.yummy.tv.domain.reviews.model.ReviewReactions
import su.afk.yummy.tv.domain.reviews.model.ReviewStatus
import su.afk.yummy.tv.domain.reviews.model.ReviewVote
import su.afk.yummy.tv.feature.reviews.tv.utils.displayReviewDate
import su.afk.yummy.tv.feature.reviews.utils.reviewStatusColor
import su.afk.yummy.tv.feature.reviews.utils.reviewStatusLabel
import su.afk.yummy.tv.feature.reviews.utils.sanitizeReviewHtml

@Composable
internal fun ReviewTvCard(
    review: AnimeReviewSummary,
    reactions: ReviewReactions,
    showAnime: Boolean,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(8.dp)
    Card(
        modifier = modifier
            .fillMaxWidth()
            .tvFocusableClick(onClick = onOpen, shape = shape),
        shape = shape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
        ) {
            if (showAnime && review.animePosterUrl != null) {
                AsyncImage(
                    model = review.animePosterUrl,
                    contentDescription = review.animeTitle,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .width(132.dp)
                        .fillMaxHeight(),
                )
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 190.dp)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (showAnime && review.animeTitle.isNotBlank()) {
                    Text(
                        text = review.animeTitle,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                if (review.status != ReviewStatus.APPROVED) {
                    Text(
                        text = review.status.reviewStatusLabel(),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = review.status.reviewStatusColor(),
                    )
                }

                Text(
                    text = sanitizeReviewHtml(review.html).htmlToPlainText(),
                    style = MaterialTheme.typography.bodyLarge,
                    fontStyle = FontStyle.Italic,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                AsyncImage(
                    model = review.author.avatarUrl,
                    contentDescription = review.author.nickname,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(8.dp)),
                )
                Column(Modifier.weight(1f)) {
                    Text(
                        text = review.author.nickname,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = review.createdAtSeconds.displayReviewDate(),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                review.rating?.average?.let { ReviewScoreBadge(it) }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                ReviewMetric(
                    icon = Icons.Filled.Visibility,
                    value = review.views,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                ReviewMetric(
                    icon = Icons.Filled.ThumbUp,
                    value = reactions.likes,
                    color = YummySemanticColors.Like,
                    emphasized = reactions.vote == ReviewVote.LIKE,
                )
                ReviewMetric(
                    icon = Icons.Filled.ThumbDown,
                    value = reactions.dislikes,
                    color = YummySemanticColors.Dislike,
                    emphasized = reactions.vote == ReviewVote.DISLIKE,
                )
            }
        }
    }
}

@Composable
private fun ReviewMetric(
    icon: ImageVector,
    value: Int,
    color: Color,
    emphasized: Boolean = false,
) {
    Row(
        modifier = Modifier.padding(horizontal = 9.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(19.dp))
        Text(
            value.toCompactCount(),
            style = MaterialTheme.typography.labelLarge,
            color = color,
            fontWeight = if (emphasized) FontWeight.Bold else FontWeight.Normal,
        )
    }
}

@Composable
private fun ReviewScoreBadge(score: Int) {
    val color = when {
        score >= 8 -> YummySemanticColors.ScoreHigh
        score >= 5 -> YummySemanticColors.ScoreMid
        else -> MaterialTheme.colorScheme.error
    }
    Surface(
        color = color,
        contentColor = YummySemanticColors.OnScoreBadge,
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Icon(Icons.Filled.Star, contentDescription = null, modifier = Modifier.size(18.dp))
            Text(
                "$score / 10",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}
