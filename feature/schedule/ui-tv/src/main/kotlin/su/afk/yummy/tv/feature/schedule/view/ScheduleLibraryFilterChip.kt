package su.afk.yummy.tv.feature.schedule.view

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.core.designsystem.focus.tvFocusableClick
import su.afk.yummy.tv.feature.schedule.presentation.R

/**
 * ТВ-переключатель фильтра «В библиотеке» на экране расписания.
 *
 * Выглядит как карточка даты рядом: та же форма, высота и цвета ([scheduleChipColors]), выбранный
 * в покое отмечен подчёркиванием, а не сплошной заливкой.
 */
@Composable
internal fun ScheduleLibraryFilterChip(
    selected: Boolean,
    focusRequester: FocusRequester,
    leftFocusRequester: FocusRequester?,
    downFocusRequester: FocusRequester?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    val chipColors = scheduleChipColors(selected = selected, focused = focused)
    val selectedIdle = selected && !focused
    val underlineColor = MaterialTheme.colorScheme.primary

    Box(
        modifier = modifier
            .defaultMinSize(minWidth = 96.dp)
            .height(ScheduleChipHeight)
            .focusRequester(focusRequester)
            .focusProperties {
                leftFocusRequester?.let { left = it }
                downFocusRequester?.let { down = it }
            }
            .tvFocusableClick(
                onClick = onClick,
                interactionSource = interactionSource,
                shape = ScheduleChipShape,
            )
            // Заливка — после tvFocusableClick, чтобы масштабироваться вместе с рамкой фокуса.
            .clip(ScheduleChipShape)
            .background(chipColors.background, ScheduleChipShape)
            // Подчёркивание рисуем, а не кладём дочерним Box: fillMaxWidth-ребёнок раздул бы чип
            // без фиксированной ширины на всю строку.
            .drawBehind {
                if (selectedIdle) {
                    val thickness = ScheduleChipUnderlineHeight.toPx()
                    drawRect(
                        color = underlineColor,
                        topLeft = Offset(0f, size.height - thickness),
                        size = Size(size.width, thickness),
                    )
                }
            },
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 14.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                imageVector = if (selected) Icons.Filled.Check else Icons.AutoMirrored.Filled.List,
                contentDescription = null,
                tint = chipColors.content,
                modifier = Modifier.size(24.dp),
            )
            Text(
                text = stringResource(R.string.schedule_filter_in_library),
                style = MaterialTheme.typography.labelLarge,
                color = chipColors.content,
                maxLines = 1,
            )
        }
    }
}
