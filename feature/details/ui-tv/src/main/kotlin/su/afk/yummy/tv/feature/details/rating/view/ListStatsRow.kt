package su.afk.yummy.tv.feature.details.rating.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.domain.account.model.AnimeListStats
import su.afk.yummy.tv.domain.account.model.UserAnimeList
import su.afk.yummy.tv.feature.details.R
import java.text.NumberFormat

@Composable
internal fun ListStatsRow(listStats: AnimeListStats) {
    val items = listOf(
        R.string.details_list_watching to listStats.count(UserAnimeList.WATCHING),
        R.string.details_list_planned to listStats.count(UserAnimeList.PLANNED),
        R.string.details_list_completed to listStats.count(UserAnimeList.COMPLETED),
        R.string.details_list_postponed to listStats.count(UserAnimeList.POSTPONED),
        R.string.details_list_dropped to listStats.count(UserAnimeList.DROPPED),
    ).filter { it.second > 0 }
    if (items.isEmpty()) return

    val integerFormat = NumberFormat.getIntegerInstance()
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        items.forEach { (labelRes, count) ->
            RatingSummaryPill(
                stringResource(
                    R.string.details_list_stat_item,
                    stringResource(labelRes),
                    integerFormat.format(count),
                )
            )
        }
    }
}

private fun AnimeListStats.count(list: UserAnimeList): Int = counts[list.id] ?: 0
