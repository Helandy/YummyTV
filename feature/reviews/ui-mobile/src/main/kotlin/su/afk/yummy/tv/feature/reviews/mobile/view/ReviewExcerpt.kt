package su.afk.yummy.tv.feature.reviews.mobile.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.core.utils.formatting.htmlToPlainText
import su.afk.yummy.tv.domain.reviews.model.AnimeReviewSummary
import su.afk.yummy.tv.domain.reviews.model.ReviewStatus
import su.afk.yummy.tv.feature.reviews.utils.sanitizeReviewHtml

@Composable
internal fun ReviewExcerpt(
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
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (review.status != ReviewStatus.APPROVED) {
            ReviewStatusBadge(review.status)
        }
        val excerpt = remember(review.html) {
            sanitizeReviewHtml(review.html).htmlToPlainText()
        }
        Text(
            text = excerpt,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = if (showAnime) 6 else 8,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
