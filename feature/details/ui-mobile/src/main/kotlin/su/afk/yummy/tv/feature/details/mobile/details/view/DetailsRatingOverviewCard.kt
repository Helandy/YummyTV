package su.afk.yummy.tv.feature.details.mobile.details.view

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.core.designsystem.components.UserRatingLabeledChip
import su.afk.yummy.tv.core.designsystem.components.toRatingTextColor
import su.afk.yummy.tv.core.designsystem.mobile.MobileSectionHeader
import su.afk.yummy.tv.core.utils.formatting.formatRating
import su.afk.yummy.tv.core.utils.formatting.toGroupedCount
import su.afk.yummy.tv.feature.details.details.model.DetailsRatingOverview
import su.afk.yummy.tv.feature.details.presentation.R

@Composable
internal fun DetailsRatingOverviewCard(
    overview: DetailsRatingOverview,
    userRating: Int?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        MobileSectionHeader(
            title = stringResource(R.string.details_rating_button),
            trailingActionLabel = if (userRating == null) stringResource(R.string.details_mobile_rate_action) else null,
            onTrailingActionClick = onClick,
            trailingContent = userRating?.let { rating -> {
                UserRatingLabeledChip(
                    rating = rating,
                    label = stringResource(R.string.details_your_rating, "").trim(),
                    onClick = onClick,
                )
            } },
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("details_rating_card")
                .clickable(onClick = onClick),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (overview.votes > 0) {
                    DetailsRatingAverage(
                        average = overview.average,
                        votes = overview.votes,
                        modifier = Modifier.weight(1f),
                    )
                    DetailsRatingHistogram(
                        distribution = overview.distribution,
                        modifier = Modifier.weight(1f),
                    )
                } else {
                    Text(
                        text = stringResource(R.string.details_mobile_rating_not_set),
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        if (overview.listCounts.isNotEmpty()) {
            MobileSectionHeader(
                title = stringResource(R.string.details_mobile_list_stats),
                modifier = Modifier.padding(top = 6.dp),
            )
            DetailsListStats(listCounts = overview.listCounts)
        }
    }
}

@Composable
private fun DetailsRatingAverage(
    average: Double?,
    votes: Int,
    modifier: Modifier = Modifier,
) {
    val color = average?.toRatingTextColor() ?: MaterialTheme.colorScheme.onSurface
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(
                imageVector = Icons.Filled.Star,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(30.dp),
            )
            Text(
                text = average?.formatRating() ?: "-",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.SemiBold,
                color = color,
                maxLines = 1,
            )
        }
        Text(
            text = stringResource(R.string.details_full_rating_votes, votes.toGroupedCount()),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
