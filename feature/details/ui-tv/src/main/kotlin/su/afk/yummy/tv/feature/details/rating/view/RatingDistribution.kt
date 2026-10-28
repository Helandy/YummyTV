package su.afk.yummy.tv.feature.details.rating.view

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.core.designsystem.components.scoreDistributionColor
import su.afk.yummy.tv.core.utils.formatting.toGroupedCount
import su.afk.yummy.tv.core.utils.formatting.formatRating
import su.afk.yummy.tv.domain.account.model.AnimeRatingBucket
import su.afk.yummy.tv.feature.details.presentation.R
import su.afk.yummy.tv.feature.details.rating.utils.weightedAverage
import kotlin.math.roundToInt

@Composable
internal fun RatingDistribution(
    distribution: List<AnimeRatingBucket>,
    headerTrailing: @Composable RowScope.() -> Unit = {},
) {
    val total = distribution.sumOf { it.count }
    if (total <= 0) {
        Row(modifier = Modifier.fillMaxWidth()) { headerTrailing() }
        return
    }

    val average = distribution.weightedAverage()

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                imageVector = Icons.Filled.Star,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp),
            )
            Text(
                text = average?.formatRating() ?: "-",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = stringResource(R.string.details_rating_votes_short, total.toGroupedCount()),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            headerTrailing()
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(28.dp),
            verticalAlignment = Alignment.Top,
        ) {
            distribution.sortedByDescending { it.rating }.chunked(5).forEach { column ->
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    column.forEach { bucket ->
                        RatingDistributionRow(
                            bucket = bucket,
                            total = total,
                            count = bucket.count.toGroupedCount(),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RatingDistributionRow(
    bucket: AnimeRatingBucket,
    total: Int,
    count: String,
) {
    val fraction = (bucket.count.toFloat() / total.toFloat()).coerceIn(0f, 1f)
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = bucket.rating.toString(),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.width(24.dp),
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .height(10.dp)
                .background(
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f),
                    RoundedCornerShape(5.dp),
                ),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction)
                    .height(10.dp)
                    .background(scoreDistributionColor(bucket.rating), RoundedCornerShape(5.dp)),
            )
        }
        Text(
            text = count,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(52.dp),
        )
        Text(
            text = stringResource(
                R.string.details_rating_percent,
                (fraction * 100).roundToInt(),
            ),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(42.dp),
        )
    }
}
