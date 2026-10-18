package su.afk.yummy.tv.feature.details.rating.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.core.utils.formatting.toGroupedCount
import su.afk.yummy.tv.domain.account.model.AnimeListStats
import su.afk.yummy.tv.domain.account.model.UserAnimeList
import su.afk.yummy.tv.feature.details.presentation.R
import su.afk.yummy.tv.feature.details.rating.utils.count
import su.afk.yummy.tv.feature.details.utils.statusColor
import su.afk.yummy.tv.feature.details.utils.statusIcon

@Composable
internal fun ListStatsRow(
    listStats: AnimeListStats,
    modifier: Modifier = Modifier,
) {
    val items = listOf(
        Triple(
            UserAnimeList.WATCHING,
            R.string.details_library_list_watching,
            listStats.count(UserAnimeList.WATCHING),
        ),
        Triple(
            UserAnimeList.PLANNED,
            R.string.details_library_list_planned,
            listStats.count(UserAnimeList.PLANNED),
        ),
        Triple(
            UserAnimeList.COMPLETED,
            R.string.details_library_list_completed,
            listStats.count(UserAnimeList.COMPLETED),
        ),
        Triple(
            UserAnimeList.POSTPONED,
            R.string.details_library_list_postponed,
            listStats.count(UserAnimeList.POSTPONED),
        ),
        Triple(
            UserAnimeList.DROPPED,
            R.string.details_library_list_dropped,
            listStats.count(UserAnimeList.DROPPED),
        ),
    ).filter { it.third > 0 }
    if (items.isEmpty()) return
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        items.forEach { (status, labelRes, count) ->
            RatingSummaryPill(
                label = stringResource(
                    R.string.details_list_stat_item,
                    stringResource(labelRes),
                    count.toGroupedCount(),
                ),
                accentColor = status.statusColor(),
                accentIcon = status.statusIcon(),
                compact = true,
            )
        }
    }
}
