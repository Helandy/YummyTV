package su.afk.yummy.tv.feature.schedule.view

import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.feature.schedule.model.ScheduleDayUi

@Composable
internal fun ScheduleDateChips(
    groups: List<ScheduleDayUi>,
    selectedEpochDay: Long?,
    chipFocusRequesters: Map<Long, FocusRequester>,
    downFocusRequester: FocusRequester,
    leftFocusRequester: FocusRequester?,
    onSelected: (Long) -> Unit,
) {
    val listState = rememberLazyListState()

    LazyRow(
        state = listState,
        modifier = Modifier.focusGroup(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp),
    ) {
        itemsIndexed(groups, key = { _, group -> group.date.toEpochDay() }) { index, group ->
            val selected = group.date.toEpochDay() == selectedEpochDay
            val epochDay = group.date.toEpochDay()
            val chipFocusRequester = chipFocusRequesters.getValue(epochDay)
            ScheduleDateChip(
                group = group,
                selected = selected,
                focusRequester = chipFocusRequester,
                downFocusRequester = downFocusRequester,
                leftFocusRequester = leftFocusRequester,
                onMoveLeft = {
                    val leftmostVisibleIndex = listState.layoutInfo.visibleItemsInfo
                        .minByOrNull { it.offset }
                        ?.index ?: 0
                    if (index <= leftmostVisibleIndex) {
                        leftFocusRequester?.requestFocus() == true
                    } else {
                        val previousEpochDay = groups.getOrNull(index - 1)?.date?.toEpochDay()
                        val previousFocusRequester = chipFocusRequesters[previousEpochDay]
                        previousFocusRequester?.requestFocus() == true
                    }
                },
                onSelected = { onSelected(epochDay) },
            )
        }
    }
}
