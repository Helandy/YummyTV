package su.afk.yummy.tv.feature.schedule.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.feature.schedule.model.ScheduleDayUi

@Composable
internal fun ScheduleMobileDateChips(
    groups: List<ScheduleDayUi>,
    selectedEpochDay: Long?,
    onSelected: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
    ) {
        items(groups, key = { it.date.toEpochDay() }) { group ->
            val epochDay = group.date.toEpochDay()
            ScheduleMobileDateChip(
                group = group,
                selected = epochDay == selectedEpochDay,
                onSelected = { onSelected(epochDay) },
            )
        }
    }
}
