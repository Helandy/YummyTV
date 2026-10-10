package su.afk.yummy.tv.feature.details.mobile.rating.view

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.domain.account.model.AnimeListStats
import su.afk.yummy.tv.domain.account.model.UserAnimeList
import su.afk.yummy.tv.feature.details.mobile.rating.utils.count
import su.afk.yummy.tv.feature.details.presentation.R
import su.afk.yummy.tv.feature.details.utils.statusColor
import java.text.NumberFormat

@Composable
internal fun MobileListStats(
    listStats: AnimeListStats,
    modifier: Modifier = Modifier,
) {
    val items = listOf(
        Triple(
            UserAnimeList.WATCHING,
            R.string.details_library_list_watching,
            listStats.count(UserAnimeList.WATCHING)
        ),
        Triple(
            UserAnimeList.PLANNED,
            R.string.details_library_list_planned,
            listStats.count(UserAnimeList.PLANNED)
        ),
        Triple(
            UserAnimeList.COMPLETED,
            R.string.details_library_list_completed,
            listStats.count(UserAnimeList.COMPLETED)
        ),
        Triple(
            UserAnimeList.POSTPONED,
            R.string.details_library_list_postponed,
            listStats.count(UserAnimeList.POSTPONED)
        ),
        Triple(
            UserAnimeList.DROPPED,
            R.string.details_library_list_dropped,
            listStats.count(UserAnimeList.DROPPED)
        ),
    ).filter { it.third > 0 }
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
            items.forEach { (status, labelRes, count) ->
                val color = status.statusColor()
                AssistChip(
                    onClick = {},
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = color.copy(alpha = 0.18f),
                        labelColor = MaterialTheme.colorScheme.onSurface,
                    ),
                    border = BorderStroke(1.dp, color.copy(alpha = 0.4f)),
                    leadingIcon = {
                        Box(
                            Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(color)
                        )
                    },
                    label = {
                        Text(
                            stringResource(
                                R.string.details_list_stat_item,
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
