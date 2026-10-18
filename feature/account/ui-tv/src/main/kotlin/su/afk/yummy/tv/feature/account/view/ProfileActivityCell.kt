package su.afk.yummy.tv.feature.account.view

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.core.designsystem.theme.YummySemanticColors
import su.afk.yummy.tv.feature.account.common.model.WatchHistoryCell

/**
 * Ячейка календаря активности. Фокус пульта выбирает день — как тап на мобилке;
 * дни после сегодняшнего рисуются пустыми и фокус не принимают.
 */
@Composable
internal fun ProfileActivityCell(
    cell: WatchHistoryCell,
    isSelected: Boolean,
    onFocused: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(8.dp)
    val background = when {
        cell.isFuture -> Color.Transparent
        cell.intensity > 0f -> YummySemanticColors.Dislike.copy(alpha = cell.intensity)
        else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.07f)
    }
    val textColor = when {
        cell.isFuture -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
        cell.intensity > 0.5f -> Color.White
        cell.intensity > 0f -> Color.White.copy(alpha = 0.85f)
        cell.isToday -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
    }
    val borderColor by animateColorAsState(
        targetValue = when {
            focused -> MaterialTheme.colorScheme.onBackground
            isSelected -> MaterialTheme.colorScheme.primary
            cell.isFuture -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)
            else -> Color.Transparent
        },
        animationSpec = tween(CELL_ANIMATION_DURATION_MS),
        label = "activityCellBorder",
    )
    val scale by animateFloatAsState(
        targetValue = if (focused) FOCUSED_CELL_SCALE else 1f,
        animationSpec = tween(CELL_ANIMATION_DURATION_MS),
        label = "activityCellScale",
    )
    Box(
        modifier = modifier
            .aspectRatio(1.15f)
            .then(
                if (cell.isFuture) {
                    Modifier
                } else {
                    Modifier
                        .onFocusChanged {
                            focused = it.isFocused
                            if (it.isFocused) onFocused()
                        }
                        .focusable()
                },
            )
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(shape)
            .background(background)
            .border(width = if (focused) 3.dp else 2.dp, color = borderColor, shape = shape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = cell.date.dayOfMonth.toString(),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (cell.isToday) FontWeight.Bold else FontWeight.Medium,
            color = textColor,
        )
    }
}

private const val CELL_ANIMATION_DURATION_MS = 150
private const val FOCUSED_CELL_SCALE = 1.08f
