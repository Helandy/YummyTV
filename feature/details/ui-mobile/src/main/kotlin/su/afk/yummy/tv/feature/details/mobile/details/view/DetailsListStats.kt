package su.afk.yummy.tv.feature.details.mobile.details.view

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.core.utils.formatting.toGroupedCount
import su.afk.yummy.tv.feature.details.details.model.DetailsListCount
import su.afk.yummy.tv.feature.details.mobile.details.utils.label
import su.afk.yummy.tv.feature.details.utils.statusColor
import su.afk.yummy.tv.feature.details.utils.statusIcon

/** Одна шкала долей списков и легенда под ней: цвет, название, число. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun DetailsListStats(
    listCounts: List<DetailsListCount>,
    modifier: Modifier = Modifier,
) {
    if (listCounts.isEmpty()) return
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(14.dp)
                .clip(RoundedCornerShape(7.dp)),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            listCounts.forEach { item ->
                Box(
                    modifier = Modifier
                        .weight(item.count.toFloat())
                        .fillMaxHeight()
                        .background(item.list.statusColor()),
                )
            }
        }
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            listCounts.forEach { item ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = item.list.statusIcon(),
                        contentDescription = null,
                        tint = item.list.statusColor(),
                        modifier = Modifier.size(20.dp),
                    )
                    Text(
                        text = item.list.label(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = item.count.toGroupedCount(),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }
    }
}
