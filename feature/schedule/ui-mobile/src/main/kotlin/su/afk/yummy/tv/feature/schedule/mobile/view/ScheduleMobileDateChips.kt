package su.afk.yummy.tv.feature.schedule.mobile.view

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.core.designsystem.mobile.tabs.MobileScrollablePillTabs
import su.afk.yummy.tv.core.designsystem.mobile.tabs.MobileTabTransition
import su.afk.yummy.tv.feature.schedule.model.ScheduleDayUi

private val DATE_CHIP_CORNER_RADIUS = 10.dp
private val DATE_CHIPS_VERTICAL_PADDING = 4.dp

@Composable
internal fun ScheduleMobileDateChips(
    groups: List<ScheduleDayUi>,
    selectedEpochDay: Long?,
    onSelected: (Long) -> Unit,
    modifier: Modifier = Modifier,
    transition: (() -> MobileTabTransition)? = null,
) {
    if (groups.isEmpty()) return
    val selectedIndex = groups
        .indexOfFirst { it.date.toEpochDay() == selectedEpochDay }
        .coerceAtLeast(0)

    MobileScrollablePillTabs(
        tabCount = groups.size,
        selectedIndex = selectedIndex,
        modifier = modifier,
        transition = transition,
        indicatorCornerRadius = DATE_CHIP_CORNER_RADIUS,
        horizontalSpacing = 10.dp,
        horizontalPadding = 16.dp,
        verticalPadding = DATE_CHIPS_VERTICAL_PADDING,
        key = { index -> groups[index].date.toEpochDay() },
    ) { index, selected, selectionFraction ->
        val group = groups[index]
        val epochDay = group.date.toEpochDay()
        ScheduleMobileDateChip(
            group = group,
            selected = selected,
            selectionFraction = selectionFraction,
            onSelected = { onSelected(epochDay) },
        )
    }
}
