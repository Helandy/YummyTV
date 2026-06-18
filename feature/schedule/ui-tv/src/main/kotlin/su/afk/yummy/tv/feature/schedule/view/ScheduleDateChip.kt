package su.afk.yummy.tv.feature.schedule.view

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.core.designsystem.presenter.focus.tvFocusableClick
import su.afk.yummy.tv.feature.schedule.model.ScheduleDayUi

@Composable
internal fun ScheduleDateChip(
    group: ScheduleDayUi,
    selected: Boolean,
    focusRequester: FocusRequester?,
    downFocusRequester: FocusRequester,
    leftFocusRequester: FocusRequester?,
    onMoveLeft: (() -> Boolean)?,
    onSelected: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    val selectedFocused = selected && focused
    val selectedIdle = selected && !focused
    val shape = RoundedCornerShape(10.dp)
    val background = when {
        selectedFocused -> MaterialTheme.colorScheme.primary
        focused -> MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
        else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)
    }
    val contentColor = when {
        selectedFocused -> MaterialTheme.colorScheme.onPrimary
        selected -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Box(
        modifier = Modifier
            .width(78.dp)
            .height(72.dp)
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .focusProperties {
                down = downFocusRequester
                leftFocusRequester?.let { left = it }
            }
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown || event.key != Key.DirectionLeft) {
                    return@onPreviewKeyEvent false
                }
                onMoveLeft?.invoke() ?: false
            }
            .onFocusChanged { if (it.isFocused) onSelected() }
            .clip(shape)
            .background(background, shape)
            .border(
                width = if (focused && !selected) 2.dp else 0.dp,
                color = if (focused && !selected) MaterialTheme.colorScheme.primary else Color.Transparent,
                shape = shape,
            )
            .tvFocusableClick(
                onClick = onSelected,
                interactionSource = interactionSource,
                shape = shape
            ),
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = group.weekdayLabel,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = contentColor,
                maxLines = 1,
            )
            Text(
                text = group.date.dayOfMonth.toString(),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
                color = contentColor,
                maxLines = 1,
            )
        }
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(8.dp)
                .size(22.dp)
                .clip(RoundedCornerShape(11.dp))
                .background(
                    color = if (selectedFocused) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.18f)
                    else MaterialTheme.colorScheme.primary.copy(alpha = 0.22f),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = group.items.size.toString(),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = contentColor,
            )
        }
        if (selectedIdle) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(4.dp)
                    .background(MaterialTheme.colorScheme.primary),
            )
        }
    }
}
