package su.afk.yummy.tv.feature.details.mobile.details.utils

import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Dp

/** Сжимает пространство вокруг элемента в списке с общим spacedBy: сдвигает его вверх на [top] и отдаёт [bottom] следующему. */
internal fun Modifier.tightenVerticalSpacing(top: Dp = Dp.Hairline, bottom: Dp = Dp.Hairline): Modifier =
    layout { measurable, constraints ->
        val placeable = measurable.measure(constraints)
        val topPx = top.roundToPx()
        val height = (placeable.height - topPx - bottom.roundToPx()).coerceAtLeast(0)
        layout(placeable.width, height) { placeable.place(0, -topPx) }
    }
