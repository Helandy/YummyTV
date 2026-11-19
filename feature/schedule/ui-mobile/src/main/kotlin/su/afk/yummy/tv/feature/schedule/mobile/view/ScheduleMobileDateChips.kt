package su.afk.yummy.tv.feature.schedule.mobile.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.core.designsystem.mobile.tabs.MobileTabTransition
import su.afk.yummy.tv.core.designsystem.mobile.tabs.animateScrollTabIntoCenter
import su.afk.yummy.tv.core.designsystem.mobile.tabs.drawLazyRowPillIndicator
import su.afk.yummy.tv.core.designsystem.mobile.tabs.rememberAnimatedTabTransition
import su.afk.yummy.tv.feature.schedule.model.ScheduleDayUi

private val DATE_CHIP_CORNER_RADIUS = 10.dp
private val DATE_CHIPS_VERTICAL_PADDING = 4.dp

@Composable
internal fun ScheduleMobileDateChips(
    groups: List<ScheduleDayUi>,
    selectedEpochDay: Long?,
    onSelected: (Long) -> Unit,
    modifier: Modifier = Modifier,
    transition: MobileTabTransition? = null,
) {
    if (groups.isEmpty()) return
    val selectedIndex = groups
        .indexOfFirst { it.date.toEpochDay() == selectedEpochDay }
        .coerceAtLeast(0)
    val resolvedTransition = transition ?: rememberAnimatedTabTransition(selectedIndex, groups.size)
    val listState = rememberLazyListState()
    val indicatorColor = MaterialTheme.colorScheme.primary
    val activeIndex = resolvedTransition.activeIndex

    LaunchedEffect(activeIndex, groups.size) {
        if (activeIndex in groups.indices) {
            listState.animateScrollTabIntoCenter(activeIndex)
        }
    }

    LazyRow(
        modifier = modifier.drawBehind {
            drawLazyRowPillIndicator(
                listState = listState,
                transition = resolvedTransition,
                color = indicatorColor,
                cornerRadius = CornerRadius(DATE_CHIP_CORNER_RADIUS.toPx()),
                verticalInsetPx = DATE_CHIPS_VERTICAL_PADDING.toPx(),
            )
        },
        state = listState,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = DATE_CHIPS_VERTICAL_PADDING),
    ) {
        itemsIndexed(groups, key = { _, group -> group.date.toEpochDay() }) { index, group ->
            val epochDay = group.date.toEpochDay()
            ScheduleMobileDateChip(
                group = group,
                selected = index == activeIndex,
                selectionFraction = resolvedTransition.selectionFraction(index),
                onSelected = { onSelected(epochDay) },
            )
        }
    }
}
