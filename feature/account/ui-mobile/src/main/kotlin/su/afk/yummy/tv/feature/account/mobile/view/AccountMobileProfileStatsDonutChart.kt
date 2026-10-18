@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package su.afk.yummy.tv.feature.account.mobile.view

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.feature.account.common.utils.positiveValueSum
import su.afk.yummy.tv.feature.account.mobile.account.utils.PROFILE_STATS_BODY_HEIGHT_DP
import su.afk.yummy.tv.feature.account.common.model.ProfileStatSlice

@Composable
internal fun AccountMobileProfileStatsDonutChart(
    slices: List<ProfileStatSlice>,
    totalLabel: String,
    percentLabel: String,
    modifier: Modifier = Modifier,
) {
    val totalValue = slices.positiveValueSum()
    val emptyColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f)
    Box(
        modifier = modifier.size(PROFILE_STATS_BODY_HEIGHT_DP.dp),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            if (totalValue <= 0L) {
                drawCircle(color = emptyColor)
                return@Canvas
            }
            var startAngle = -90f
            slices.forEach { slice ->
                val sweepAngle = 360f * slice.value.toFloat() / totalValue.toFloat()
                drawArc(
                    color = slice.color,
                    startAngle = startAngle,
                    sweepAngle = sweepAngle,
                    useCenter = true,
                )
                startAngle += sweepAngle
            }
        }
        Column(
            modifier = Modifier
                .size(58.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.94f)),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = totalLabel,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                maxLines = 1,
            )
            Text(
                text = percentLabel,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 1,
            )
        }
    }
}
