@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package su.afk.yummy.tv.feature.account.mobile.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.domain.account.model.UserProfileSummary
import su.afk.yummy.tv.domain.account.model.UserStats
import su.afk.yummy.tv.feature.account.mobile.account.utils.hasAny

@Composable
internal fun AccountMobileProfileSummaryPanel(
    summary: UserProfileSummary,
    stats: UserStats?,
) {
    AccountMobileSurfacePanel {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            AccountMobileProfileStatsPager(summary = summary, stats = stats)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                AccountMobileDaysOnlineTile(daysOnline = summary.daysOnline)
                AccountMobileProfileHeatmap(
                    history = summary.watchHistory,
                    modifier = Modifier.weight(1f),
                )
            }
            if (summary.counts.hasAny()) {
                AccountMobileProfileListCounters(counts = summary.counts)
            }
            if (summary.about.isNotBlank()) {
                Text(
                    text = summary.about,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
