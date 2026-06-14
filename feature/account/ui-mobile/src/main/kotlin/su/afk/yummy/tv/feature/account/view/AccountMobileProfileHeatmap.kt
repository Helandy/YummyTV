package su.afk.yummy.tv.feature.account.view

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.domain.account.model.UserWatchHistoryDay
import kotlin.math.ceil
import kotlin.math.min

@Composable
internal fun AccountMobileProfileHeatmap(
    history: List<UserWatchHistoryDay>,
    modifier: Modifier = Modifier,
    maxDays: Int = 98,
) {
    val days = history.takeLast(maxDays)
    val maxDuration = days.maxOfOrNull { it.durationSeconds }?.coerceAtLeast(1L) ?: 1L
    val activeColor = Color(0xFFFF6B6B)
    val emptyColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.09f)

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(74.dp),
    ) {
        if (days.isEmpty()) return@Canvas
        val rows = HEATMAP_ROWS
        val columns = ceil(days.size / rows.toFloat()).toInt().coerceAtLeast(1)
        val gap = 4.dp.toPx()
        val cell = min(
            (size.width - gap * (columns - 1)) / columns,
            (size.height - gap * (rows - 1)) / rows,
        ).coerceAtLeast(1f)
        val startX = (size.width - (columns * cell + (columns - 1) * gap)) / 2f
        val startY = (size.height - (rows * cell + (rows - 1) * gap)) / 2f
        days.forEachIndexed { index, day ->
            val column = index / rows
            val row = index % rows
            val alpha = if (day.durationSeconds <= 0L) {
                1f
            } else {
                (0.32f + 0.68f * day.durationSeconds.toFloat() / maxDuration.toFloat()).coerceIn(
                    0.32f,
                    1f
                )
            }
            drawRoundRect(
                color = if (day.durationSeconds > 0L) activeColor.copy(alpha = alpha) else emptyColor,
                topLeft = Offset(
                    x = startX + column * (cell + gap),
                    y = startY + row * (cell + gap),
                ),
                size = Size(cell, cell),
                cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx()),
            )
        }
    }
}

private const val HEATMAP_ROWS = 7
