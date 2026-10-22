package su.afk.yummy.tv.feature.details.mobile.rating.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.core.designsystem.components.UserRatingLabeledChip
import su.afk.yummy.tv.core.utils.formatting.toGroupedCount
import su.afk.yummy.tv.core.utils.formatting.formatRating
import su.afk.yummy.tv.domain.account.model.AnimeRatingSummary
import su.afk.yummy.tv.feature.details.utils.weightedAverage
import su.afk.yummy.tv.feature.details.presentation.R

@Composable
internal fun MobileRatingOverview(
    ratingSummary: AnimeRatingSummary,
    selectedUserRating: Int?,
    modifier: Modifier = Modifier,
) {
    val total = ratingSummary.distribution.sumOf { it.count }
    val average = ratingSummary.weightedAverage()

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Star,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = average?.formatRating() ?: "-",
                            style = MaterialTheme.typography.displaySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                    Text(
                        text = stringResource(
                            R.string.details_full_rating_votes,
                            total.toGroupedCount(),
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (selectedUserRating != null) {
                    UserRatingLabeledChip(
                        rating = selectedUserRating,
                        label = stringResource(R.string.details_your_rating, "").trim(),
                        modifier = Modifier.padding(start = 12.dp),
                    )
                } else {
                    Text(
                        text = stringResource(R.string.details_mobile_rating_not_set),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.End,
                        modifier = Modifier.padding(start = 12.dp),
                    )
                }
            }
        }
    }
}
