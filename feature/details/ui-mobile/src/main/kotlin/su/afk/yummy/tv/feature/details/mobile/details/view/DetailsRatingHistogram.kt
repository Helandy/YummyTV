package su.afk.yummy.tv.feature.details.mobile.details.view

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.core.designsystem.components.scoreDistributionColor

/** Мини-гистограмма голосов за оценки 1..10: индекс = оценка − 1. */
@Composable
internal fun DetailsRatingHistogram(
    distribution: List<Int>,
    modifier: Modifier = Modifier,
) {
    val max = distribution.maxOrNull()?.takeIf { it > 0 } ?: return
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            distribution.forEachIndexed { index, count ->
                val fraction = (count.toFloat() / max).coerceAtLeast(0.06f)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(fraction)
                        .background(
                            color = scoreDistributionColor(index + 1)
                                .copy(alpha = if (count > 0) 0.9f else 0.25f),
                            shape = RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp),
                        ),
                )
            }
        }
        Row(modifier = Modifier.fillMaxWidth()) {
            listOf("1", "5", "10").forEachIndexed { index, label ->
                Text(
                    text = label,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = when (index) {
                        0 -> TextAlign.Start
                        1 -> TextAlign.Center
                        else -> TextAlign.End
                    },
                )
            }
        }
    }
}
