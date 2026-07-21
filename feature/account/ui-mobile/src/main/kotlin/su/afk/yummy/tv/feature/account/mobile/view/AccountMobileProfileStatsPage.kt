@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package su.afk.yummy.tv.feature.account.mobile.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.feature.account.mobile.R
import su.afk.yummy.tv.feature.account.mobile.account.model.AccountMobileProfileStatsPageModel
import su.afk.yummy.tv.feature.account.mobile.account.utils.positiveValueSum
import su.afk.yummy.tv.feature.account.mobile.account.utils.totalLabel

@Composable
internal fun AccountMobileProfileStatsPage(
    page: AccountMobileProfileStatsPageModel,
    modifier: Modifier = Modifier,
) {
    val positiveSlices = page.slices.filter { it.value > 0L }
    val totalValue = positiveSlices.positiveValueSum()
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = page.title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        if (page.slices.isEmpty()) {
            AccountMobileProfileStatsEmptyState()
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                AccountMobileProfileStatsDonutChart(
                    slices = positiveSlices,
                    totalLabel = page.valueType.totalLabel(totalValue),
                    percentLabel = stringResource(R.string.account_profile_percent_full),
                )
                AccountMobileProfileStatsLegend(
                    slices = page.slices,
                    valueType = page.valueType,
                    compact = page.compactLegend,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}
