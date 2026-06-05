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
import su.afk.yummy.tv.feature.details.mobile.R
import java.text.NumberFormat
import kotlin.math.roundToInt

@Composable
internal fun MobileRatingDistribution(
    distribution: List<AnimeRatingBucket>,
    modifier: Modifier = Modifier,
) {
    val total = distribution.sumOf { it.count }
    val integerFormat = NumberFormat.getIntegerInstance()

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = stringResource(R.string.details_mobile_rating_distribution),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
        )
        if (total <= 0) {
            Text(
                text = stringResource(R.string.details_mobile_rating_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            return@Column
        }
        distribution.sortedByDescending { it.rating }.forEach { bucket ->
            val fraction = bucket.count.toFloat() / total.toFloat()
            MobileRatingDistributionRow(
                bucket = bucket,
                fraction = fraction.coerceIn(0f, 1f),
                count = integerFormat.format(bucket.count),
            )
        }
    }
}

@Composable
private fun MobileRatingDistributionRow(
    bucket: AnimeRatingBucket,
    fraction: Float,
    count: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = bucket.rating.toString(),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.width(20.dp),
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .height(10.dp)
                .background(
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f),
                    RoundedCornerShape(5.dp)
                ),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction)
                    .height(10.dp)
                    .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(5.dp)),
            )
        }
        Text(
            text = count,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(42.dp),
        )
        Text(
            text = stringResource(
                R.string.details_mobile_rating_percent,
                (fraction * 100).roundToInt()
            ),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(40.dp),
        )
    }
}
