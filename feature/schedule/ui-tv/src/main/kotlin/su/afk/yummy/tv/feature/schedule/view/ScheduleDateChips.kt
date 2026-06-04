package su.afk.yummy.tv.feature.schedule.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.feature.schedule.model.ScheduleDayUi

@Composable
internal fun ScheduleDateChips(
    groups: List<ScheduleDayUi>,
    selectedEpochDay: Long,
    selectedFocusRequester: FocusRequester,
    downFocusRequester: FocusRequester,
    leftFocusRequester: FocusRequester?,
    onSelected: (Long) -> Unit,
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp),
    ) {
        itemsIndexed(groups, key = { _, group -> group.date.toEpochDay() }) { _, group ->
            val selected = group.date.toEpochDay() == selectedEpochDay
            ScheduleDateChip(
                group = group,
                selected = selected,
                focusRequester = if (selected) selectedFocusRequester else null,
                downFocusRequester = downFocusRequester,
                leftFocusRequester = leftFocusRequester,
                onSelected = { onSelected(group.date.toEpochDay()) },
            )
        }
    }
}
