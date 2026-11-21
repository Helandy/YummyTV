package su.afk.yummy.tv.feature.schedule.view

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/** Форма карточек верхней строки расписания (чип фильтра и чипы дат). */
internal val ScheduleChipShape = RoundedCornerShape(10.dp)

/** Общая высота карточек верхней строки расписания, чтобы фильтр и даты стояли на одной линии. */
internal val ScheduleChipHeight = 72.dp

/** Толщина подчёркивания выбранной карточки в покое. */
internal val ScheduleChipUnderlineHeight = 4.dp

/** Цвета карточки верхней строки расписания для текущего состояния. */
@Immutable
internal data class ScheduleChipColors(
    val background: Color,
    val content: Color,
)

/**
 * Цвета карточки: выбранная в фокусе — сплошной `primary`, выбранная в покое — тинтованный контент,
 * в фокусе — мягкая подсветка `primary`, иначе нейтральная тональная заливка.
 */
@Composable
internal fun scheduleChipColors(selected: Boolean, focused: Boolean): ScheduleChipColors {
    val scheme = MaterialTheme.colorScheme
    val selectedFocused = selected && focused
    return ScheduleChipColors(
        background = when {
            selectedFocused -> scheme.primary
            focused -> scheme.primary.copy(alpha = 0.18f)
            else -> scheme.onSurface.copy(alpha = 0.06f)
        },
        content = when {
            selectedFocused -> scheme.onPrimary
            selected -> scheme.primary
            else -> scheme.onSurfaceVariant
        },
    )
}
