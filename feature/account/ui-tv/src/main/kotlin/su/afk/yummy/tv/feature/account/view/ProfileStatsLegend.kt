package su.afk.yummy.tv.feature.account.view

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.feature.account.account.model.ProfileStatsValueType
import su.afk.yummy.tv.feature.account.common.model.ProfileStatSlice
import su.afk.yummy.tv.feature.account.utils.PROFILE_STATS_LEGEND_MAX_ROWS
import su.afk.yummy.tv.feature.account.utils.valueLabel

/** Легенда колонками, как на мобилке: не больше [PROFILE_STATS_LEGEND_MAX_ROWS] рядов. */
@Composable
internal fun ProfileStatsLegend(
    slices: List<ProfileStatSlice>,
    valueType: ProfileStatsValueType,
    columns: Int,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
    ) {
        slices.take(columns * PROFILE_STATS_LEGEND_MAX_ROWS).chunked(columns).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                row.forEach { slice ->
                    LegendItem(
                        slice = slice,
                        valueType = valueType,
                        modifier = Modifier.weight(1f),
                    )
                }
                // Неполный ряд не растягивает элементы на всю ширину.
                repeat(columns - row.size) { Spacer(modifier = Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun LegendItem(
    slice: ProfileStatSlice,
    valueType: ProfileStatsValueType,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        Box(
            modifier = Modifier
                .size(11.dp)
                .clip(CircleShape)
                .background(slice.color),
        )
        Text(
            text = slice.title,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = valueType.valueLabel(slice.value),
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
        )
    }
}
