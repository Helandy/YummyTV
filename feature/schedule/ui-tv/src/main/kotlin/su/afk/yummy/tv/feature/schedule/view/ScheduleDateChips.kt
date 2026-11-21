package su.afk.yummy.tv.feature.schedule.view

import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import su.afk.yummy.tv.feature.schedule.model.ScheduleDayUi

/**
 * Пауза без новых фокус-событий перед сменой списка по фокусу на дате. Пока зажата стрелка, список
 * (и его постеры) не пересобирается на каждый шаг, а один раз — на дате, где остановились.
 */
private const val FocusSelectionDebounceMillis = 100L

@Composable
internal fun ScheduleDateChips(
    groups: List<ScheduleDayUi>,
    selectedEpochDay: Long?,
    chipFocusRequesters: Map<Long, FocusRequester>,
    downFocusRequester: FocusRequester,
    leftFocusRequester: FocusRequester?,
    onSelected: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    val currentOnSelected by rememberUpdatedState(onSelected)
    val currentSelectedEpochDay by rememberUpdatedState(selectedEpochDay)
    var pendingEpochDay by remember { mutableStateOf<Long?>(null) }

    LaunchedEffect(pendingEpochDay) {
        val epochDay = pendingEpochDay ?: return@LaunchedEffect
        delay(FocusSelectionDebounceMillis)
        pendingEpochDay = null
        currentOnSelected(epochDay)
    }

    fun commitPending(): Boolean {
        val epochDay = pendingEpochDay ?: return false
        pendingEpochDay = null
        currentOnSelected(epochDay)
        return true
    }

    LazyRow(
        state = listState,
        modifier = modifier
            .focusGroup()
            // DPAD-вниз до применения выбора увёл бы фокус в список старой даты, который вот-вот
            // удалится. Сначала применяем выбор и гасим нажатие: следующее попадёт в новый список.
            .onPreviewKeyEvent { event ->
                event.type == KeyEventType.KeyDown && event.key == Key.DirectionDown && commitPending()
            },
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
                onFocused = {
                    // Фокус на уже выбранной дате ничего не меняет и отменяет отложенный выбор другой.
                    pendingEpochDay = epochDay.takeIf { it != currentSelectedEpochDay }
                },
                onSelected = {
                    pendingEpochDay = null
                    currentOnSelected(epochDay)
                },
            )
        }
    }
}
