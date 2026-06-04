package su.afk.yummy.tv.feature.details.rating.view

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.domain.account.model.AnimeRatingBucket
import su.afk.yummy.tv.feature.details.R

@Composable
internal fun RatingDistribution(distribution: List<AnimeRatingBucket>) {
    val total = distribution.sumOf { it.count }
    if (total <= 0) {
        Text(
            text = stringResource(R.string.details_rating_empty),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        return
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(R.string.details_rating_votes_short, total),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
        )
        distribution.sortedByDescending { it.rating }.forEach { bucket ->
            val fraction = bucket.count.toFloat() / total.toFloat()
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = bucket.rating.toString(),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.width(24.dp),
                )
                Box(
                    modifier = Modifier
                        .width(360.dp)
                        .height(10.dp)
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f), RoundedCornerShape(5.dp)),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(fraction.coerceIn(0f, 1f))
                            .height(10.dp)
                            .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(5.dp)),
                    )
                }
                Text(
                    text = bucket.count.toString(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
