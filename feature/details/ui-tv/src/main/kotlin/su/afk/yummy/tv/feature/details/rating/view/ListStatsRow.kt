package su.afk.yummy.tv.feature.details.rating.view

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import su.afk.yummy.tv.domain.account.model.AnimeListStats
import su.afk.yummy.tv.feature.details.R

@Composable
internal fun ListStatsRow(listStats: AnimeListStats) {
    val watching = listStats.counts[0] ?: 0
    val planned = listStats.counts[1] ?: 0
    val completed = listStats.counts[2] ?: 0
    if (watching + planned + completed <= 0) return
    RatingSummaryPill(stringResource(R.string.details_list_stats, watching, planned, completed))
}
