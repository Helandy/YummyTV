@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package su.afk.yummy.tv.feature.account.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.feature.account.R
import su.afk.yummy.tv.feature.account.account.model.ProfileStatsPageModel
import su.afk.yummy.tv.feature.account.utils.positiveValueSum
import su.afk.yummy.tv.feature.account.utils.totalLabel

@Composable
internal fun ProfileStatsCard(
    page: ProfileStatsPageModel,
    focused: Boolean,
    modifier: Modifier = Modifier,
) {
    val positiveSlices = remember(page.slices) { page.slices.filter { it.value > 0L } }
    val totalValue = remember(positiveSlices) { positiveSlices.positiveValueSum() }
    val shape = RoundedCornerShape(12.dp)

    Column(
        modifier = modifier
            .height(252.dp)
            .profileTileVisual(
                focused = focused,
                shape = shape,
                focusedScale = 1.015f,
                unfocusedContainerAlpha = 0.055f,
            )
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                imageVector = page.icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                modifier = Modifier.size(18.dp),
            )
            Text(
                text = page.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (totalValue <= 0L) {
            ProfileStatsEmptyState(modifier = Modifier.weight(1f))
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                ProfileStatsDonutChart(
                    slices = positiveSlices,
                    totalLabel = page.valueType.totalLabel(totalValue),
                    percentLabel = stringResource(R.string.account_profile_percent_full),
                )
                ProfileStatsLegend(
                    slices = page.slices,
                    valueType = page.valueType,
                    compact = page.compactLegend,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}
