package su.afk.yummy.tv.feature.reviews.mobile.view

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import su.afk.yummy.tv.core.designsystem.presenter.preview.ScreenPreviewTheme
import su.afk.yummy.tv.core.designsystem.presenter.theme.YummySemanticColors
import su.afk.yummy.tv.core.utils.htmlToPlainText
import su.afk.yummy.tv.domain.reviews.model.AnimeReviewSummary
import su.afk.yummy.tv.domain.reviews.model.ReviewAuthor
import su.afk.yummy.tv.domain.reviews.model.ReviewRating
import su.afk.yummy.tv.domain.reviews.model.ReviewReactions
import su.afk.yummy.tv.domain.reviews.model.ReviewStatus
import su.afk.yummy.tv.domain.reviews.model.ReviewVote
import su.afk.yummy.tv.feature.reviews.mobile.R
import su.afk.yummy.tv.feature.reviews.mobile.utils.displayCompactReviewCount
import su.afk.yummy.tv.feature.reviews.mobile.utils.displayReviewDate
import su.afk.yummy.tv.feature.reviews.utils.sanitizeReviewHtml

@Composable
internal fun ReviewMobileCard(
    review: AnimeReviewSummary,
    reactions: ReviewReactions,
    showAnime: Boolean,
    onOpen: () -> Unit,
    onAuthor: () -> Unit,
    onVote: (ReviewVote) -> Unit,
) {
    ElevatedCard(
        onClick = onOpen,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
    ) {
        if (showAnime && review.animePosterUrl != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 164.dp),
            ) {
                AsyncImage(
                    model = review.animePosterUrl,
                    contentDescription = review.animeTitle,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .width(108.dp)
                        .height(172.dp),
                )
                ReviewExcerpt(
                    review = review,
                    showAnime = true,
                    modifier = Modifier
                        .weight(1f)
                        .padding(14.dp),
                )
            }
        } else {
            ReviewExcerpt(
                review = review,
                showAnime = showAnime,
                modifier = Modifier.padding(16.dp),
            )
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            ReviewAuthorRow(review = review, onAuthor = onAuthor)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                ReviewMetric(
                    icon = Icons.Filled.Visibility,
                    value = review.views,
                    contentDescription = stringResource(R.string.review_views, review.views),
                )
                ReviewMetric(
                    icon = Icons.Filled.ThumbUp,
                    value = reactions.likes,
                    contentDescription = "${stringResource(R.string.review_like)} ${reactions.likes}",
                    accentColor = YummySemanticColors.Like,
                    selected = reactions.vote == ReviewVote.LIKE,
                    onClick = {
                        onVote(
                            if (reactions.vote == ReviewVote.LIKE) ReviewVote.NONE else ReviewVote.LIKE
                        )
                    },
                )
                ReviewMetric(
                    icon = Icons.Filled.ThumbDown,
                    value = reactions.dislikes,
                    contentDescription = "${stringResource(R.string.review_dislike)} ${reactions.dislikes}",
                    accentColor = YummySemanticColors.Dislike,
                    selected = reactions.vote == ReviewVote.DISLIKE,
                    onClick = {
                        onVote(
                            if (reactions.vote == ReviewVote.DISLIKE) ReviewVote.NONE else ReviewVote.DISLIKE
                        )
                    },
                )
            }
        }
    }
}

@Composable
private fun ReviewExcerpt(
    review: AnimeReviewSummary,
    showAnime: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (showAnime && review.animeTitle.isNotBlank()) {
            Text(
                text = review.animeTitle,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (review.status != ReviewStatus.APPROVED) {
            ReviewStatusBadge(review.status)
        }
        Text(
            text = sanitizeReviewHtml(review.html).htmlToPlainText(),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = if (showAnime) 6 else 8,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun ReviewStatusBadge(status: ReviewStatus) {
    val color = status.reviewStatusColor()
    Surface(
        color = color.copy(alpha = 0.12f),
        contentColor = color,
        shape = RoundedCornerShape(50),
    ) {
        Text(
            text = status.reviewStatusLabel(),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
        )
    }
}

@Composable
private fun ReviewAuthorRow(
    review: AnimeReviewSummary,
    onAuthor: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .clickable(onClick = onAuthor),
            contentAlignment = Alignment.Center,
        ) {
            if (review.author.avatarUrl != null) {
                AsyncImage(
                    model = review.author.avatarUrl,
                    contentDescription = review.author.nickname,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.matchParentSize(),
                )
            } else {
                Icon(
                    imageVector = Icons.Filled.Person,
                    contentDescription = review.author.nickname,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(22.dp),
                )
            }
        }
        Column(Modifier.weight(1f)) {
            Text(
                text = review.author.nickname,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.clickable(onClick = onAuthor),
            )
            Text(
                text = review.createdAtSeconds.displayReviewDate(),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        review.rating?.average?.let { ReviewScoreBadge(it) }
    }
}

@Composable
private fun ReviewMetric(
    icon: ImageVector,
    value: Int,
    contentDescription: String,
    accentColor: Color? = null,
    selected: Boolean = false,
    onClick: (() -> Unit)? = null,
) {
    val color = accentColor ?: MaterialTheme.colorScheme.onSurfaceVariant
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (selected) color.copy(alpha = 0.16f) else Color.Transparent
            )
            .let { if (onClick != null) it.clickable(onClick = onClick) else it }
            .padding(horizontal = 9.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = color,
            modifier = Modifier.size(17.dp),
        )
        Text(
            text = value.displayCompactReviewCount(),
            style = MaterialTheme.typography.labelLarge,
            color = color,
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
        shape = RoundedCornerShape(8.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(Icons.Filled.Star, contentDescription = null, modifier = Modifier.size(16.dp))
            Text(
                text = "$score / 10",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

private val previewReview = AnimeReviewSummary(
    id = 1,
    animeId = 1,
    status = ReviewStatus.APPROVED,
    author = ReviewAuthor(id = 1, nickname = "Mori", avatarUrl = null),
    createdAtSeconds = 1_750_000_000,
    updatedAtSeconds = 1_750_000_000,
    views = 1_248,
    rating = ReviewRating(average = 9, categories = emptyList()),
    reactions = ReviewReactions(likes = 84, dislikes = 3, vote = ReviewVote.LIKE),
    html = "История, которая не торопится раскрывать свои секреты и бережно работает с персонажами.",
    checkComment = null,
    commentable = true,
    animeTitle = "Провожающая в последний путь Фрирен",
)

@Preview(name = "General feed", showBackground = true)
@Composable
private fun ReviewMobileCardGeneralPreview() = ScreenPreviewTheme {
    ReviewMobileCard(
        review = previewReview,
        reactions = previewReview.reactions,
        showAnime = true,
        onOpen = {},
        onAuthor = {},
        onVote = {},
    )
}

@Preview(name = "Anime reviews", showBackground = true)
@Composable
private fun ReviewMobileCardAnimePreview() = ScreenPreviewTheme {
    ReviewMobileCard(
        review = previewReview.copy(status = ReviewStatus.WAITING),
        reactions = previewReview.reactions.copy(vote = ReviewVote.NONE),
        showAnime = false,
        onOpen = {},
        onAuthor = {},
        onVote = {},
    )
}
