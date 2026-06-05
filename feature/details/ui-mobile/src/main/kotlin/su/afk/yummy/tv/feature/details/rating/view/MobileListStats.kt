package su.afk.yummy.tv.feature.details.rating.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.AssistChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.domain.account.model.AnimeListStats
import su.afk.yummy.tv.feature.details.mobile.R
import java.text.NumberFormat

@Composable
internal fun MobileListStats(
    listStats: AnimeListStats,
    modifier: Modifier = Modifier,
) {
    val items = listOf(
        R.string.details_mobile_library_watching to (listStats.counts[0] ?: 0),
        R.string.details_mobile_library_planned to (listStats.counts[1] ?: 0),
        R.string.details_mobile_library_completed to (listStats.counts[2] ?: 0),
        R.string.details_mobile_library_postponed to (listStats.counts[3] ?: 0),
        R.string.details_mobile_library_dropped to (listStats.counts[4] ?: 0),
    ).filter { it.second > 0 }
    if (items.isEmpty()) return

    val integerFormat = NumberFormat.getIntegerInstance()
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = stringResource(R.string.details_mobile_list_stats),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items.forEach { (labelRes, count) ->
                AssistChip(
                    onClick = {},
                    label = { Text("${stringResource(labelRes)} ${integerFormat.format(count)}") },
                )
            }
        }
    }
}
