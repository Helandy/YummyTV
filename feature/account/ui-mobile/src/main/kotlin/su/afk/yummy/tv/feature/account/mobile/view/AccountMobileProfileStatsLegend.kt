package su.afk.yummy.tv.feature.account.mobile.view

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.PersistentList
import su.afk.yummy.tv.feature.account.mobile.account.model.AccountMobileProfileStatsValueType
import su.afk.yummy.tv.feature.account.mobile.account.utils.valueLabel
import su.afk.yummy.tv.feature.account.common.model.ProfileStatSlice

@Composable
internal fun AccountMobileProfileStatsLegend(
    slices: PersistentList<ProfileStatSlice>,
    valueType: AccountMobileProfileStatsValueType,
    columns: Int,
    modifier: Modifier = Modifier,
) {
    val rows = remember(slices, columns) { slices.chunked(columns) }
    // Шесть рядов при 8dp не помещаются в высоту кольца.
    val rowSpacing = if (rows.size > 5) 4.dp else 8.dp
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = if (rows.size > LEGEND_FIT_ROWS) {
            Arrangement.spacedBy(rowSpacing)
        } else {
            Arrangement.spacedBy(rowSpacing, Alignment.CenterVertically)
        },
    ) {
        items(rows, key = { row -> row.first().title }) { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                row.forEach { slice ->
                    AccountMobileProfileStatsLegendItem(
                        slice = slice,
                        valueType = valueType,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

/** Рядов легенды, которые помещаются в высоту кольца без прокрутки. */
private const val LEGEND_FIT_ROWS = 6

@Composable
private fun AccountMobileProfileStatsLegendItem(
    slice: ProfileStatSlice,
    valueType: AccountMobileProfileStatsValueType,
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
