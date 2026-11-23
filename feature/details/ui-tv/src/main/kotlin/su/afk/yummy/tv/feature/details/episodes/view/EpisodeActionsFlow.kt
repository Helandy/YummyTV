package su.afk.yummy.tv.feature.details.episodes.view

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.dp

/** Переносит кнопки по их ширине и распределяет свободное место внутри каждой строки. */
@Composable
internal fun EpisodeActionsFlow(content: @Composable () -> Unit) {
    Layout(content = content, modifier = Modifier) { measurables, constraints ->
        val spacing = 12.dp.roundToPx()
        val width = constraints.maxWidth
        val rows = mutableListOf<MutableList<Pair<Int, Int>>>()
        measurables.forEachIndexed { index, measurable ->
            val itemWidth = measurable.maxIntrinsicWidth(Constraints.Infinity).coerceAtMost(width)
            val row = rows.lastOrNull()
            val usedWidth = row?.sumOf { it.second } ?: 0
            if (row == null || usedWidth + spacing * row.size + itemWidth > width) {
                rows.add(mutableListOf(index to itemWidth))
            } else {
                row.add(index to itemWidth)
            }
        }

        var y = 0
        val positions = mutableListOf<Triple<Placeable, Int, Int>>()
        rows.forEach { row ->
            val remaining = width - row.sumOf { it.second } - spacing * (row.size - 1)
            var x = 0
            var rowHeight = 0
            row.forEachIndexed { position, (index, naturalWidth) ->
                val itemWidth = naturalWidth + remaining / row.size + if (position < remaining % row.size) 1 else 0
                val placeable = measurables[index].measure(Constraints.fixedWidth(itemWidth))
                positions.add(Triple(placeable, x, y))
                x += itemWidth + spacing
                rowHeight = maxOf(rowHeight, placeable.height)
            }
            y += rowHeight + spacing
        }
        val height = (y - if (rows.isEmpty()) 0 else spacing).coerceAtLeast(0)
        layout(width, constraints.constrainHeight(height)) {
            positions.forEach { (placeable, x, top) -> placeable.placeRelative(x, top) }
        }
    }
}
