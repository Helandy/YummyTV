package su.afk.yummy.tv.feature.account.mobile.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.domain.account.model.UserProfileSummary
import su.afk.yummy.tv.domain.account.model.UserStats

@Composable
internal fun AccountMobileProfileSummaryPanel(
    summary: UserProfileSummary,
    stats: UserStats?,
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        AccountMobileSurfacePanel {
            AccountMobileProfileHeatmap(
                history = summary.watchHistory,
                daysOnline = summary.daysOnline,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        AccountMobileSurfacePanel {
            AccountMobileProfileStatsPager(summary = summary, stats = stats)
        }
    }
}
