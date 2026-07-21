package su.afk.yummy.tv.feature.details.mobile.rating.view

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
import su.afk.yummy.tv.domain.account.model.UserAnimeList
import su.afk.yummy.tv.feature.details.mobile.R
import java.text.NumberFormat

@Composable
internal fun MobileListStats(
    listStats: AnimeListStats,
    modifier: Modifier = Modifier,
) {
    val items = listOf(
        R.string.details_mobile_library_watching to listStats.count(UserAnimeList.WATCHING),
        R.string.details_mobile_library_planned to listStats.count(UserAnimeList.PLANNED),
        R.string.details_mobile_library_completed to listStats.count(UserAnimeList.COMPLETED),
        R.string.details_mobile_library_postponed to listStats.count(UserAnimeList.POSTPONED),
        R.string.details_mobile_library_dropped to listStats.count(UserAnimeList.DROPPED),
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
                    label = {
                        Text(
                            stringResource(
                                R.string.details_mobile_list_stat_item,
                                stringResource(labelRes),
                                integerFormat.format(count),
                            )
                        )
                    },
                )
            }
        }
    }
}

private fun AnimeListStats.count(list: UserAnimeList): Int = counts[list.id] ?: 0
