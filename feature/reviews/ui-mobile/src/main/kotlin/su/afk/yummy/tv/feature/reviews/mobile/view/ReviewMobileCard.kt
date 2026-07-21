package su.afk.yummy.tv.feature.reviews.mobile.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import su.afk.yummy.tv.core.designsystem.presenter.preview.ScreenPreviewTheme
import su.afk.yummy.tv.core.designsystem.presenter.theme.YummySemanticColors
import su.afk.yummy.tv.domain.reviews.model.AnimeReviewSummary
import su.afk.yummy.tv.domain.reviews.model.ReviewAuthor
import su.afk.yummy.tv.domain.reviews.model.ReviewRating
import su.afk.yummy.tv.domain.reviews.model.ReviewReactions
import su.afk.yummy.tv.domain.reviews.model.ReviewStatus
import su.afk.yummy.tv.domain.reviews.model.ReviewVote
import su.afk.yummy.tv.feature.reviews.mobile.R

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

internal val previewReview = AnimeReviewSummary(
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
internal fun ReviewMobileCardGeneralPreview() = ScreenPreviewTheme {
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
internal fun ReviewMobileCardAnimePreview() = ScreenPreviewTheme {
    ReviewMobileCard(
        review = previewReview.copy(status = ReviewStatus.WAITING),
        reactions = previewReview.reactions.copy(vote = ReviewVote.NONE),
        showAnime = false,
        onOpen = {},
        onAuthor = {},
        onVote = {},
    )
}
