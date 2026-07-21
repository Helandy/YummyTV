package su.afk.yummy.tv.feature.player.mobile.view.tutorial

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke

internal fun DrawScope.drawTutorialScreenFrame() {
    drawRoundRect(
        color = Color.White.copy(alpha = 0.18f),
        cornerRadius = CornerRadius(24f),
        style = Stroke(width = 4f),
    )
}

internal fun DrawScope.drawSeekChevrons(
    center: Offset,
    pointsRight: Boolean,
    color: Color,
) {
    val direction = if (pointsRight) 1f else -1f
    repeat(2) { index ->
        val x = center.x + direction * (28f + index * 25f)
        drawLine(
            color = color,
            start = Offset(x - direction * 12f, center.y - 17f),
            end = Offset(x, center.y),
            strokeWidth = 5f,
            cap = StrokeCap.Round,
        )
        drawLine(
            color = color,
            start = Offset(x, center.y),
            end = Offset(x - direction * 12f, center.y + 17f),
            strokeWidth = 5f,
            cap = StrokeCap.Round,
        )
    }
}

internal fun DrawScope.drawVerticalRail(
    x: Float,
    progress: Float,
    accent: Color,
) {
    val railTop = size.height * 0.18f
    val railHeight = size.height * 0.64f
    drawRoundRect(
        color = Color.White.copy(alpha = 0.18f),
        topLeft = Offset(x - 8f, railTop),
        size = Size(16f, railHeight),
        cornerRadius = CornerRadius(10f),
    )
    val fillHeight = railHeight * (0.22f + progress * 0.68f)
    drawRoundRect(
        color = accent,
        topLeft = Offset(x - 8f, railTop + railHeight - fillHeight),
        size = Size(16f, fillHeight),
        cornerRadius = CornerRadius(10f),
    )
}

internal fun DrawScope.drawVerticalArrow(
    x: Float,
    color: Color,
) {
    val top = size.height * 0.27f
    val bottom = size.height * 0.73f
    drawLine(color, Offset(x, bottom), Offset(x, top), 5f, StrokeCap.Round)
    drawLine(color, Offset(x, top), Offset(x - 12f, top + 16f), 5f, StrokeCap.Round)
    drawLine(color, Offset(x, top), Offset(x + 12f, top + 16f), 5f, StrokeCap.Round)
    drawLine(color, Offset(x, bottom), Offset(x - 12f, bottom - 16f), 5f, StrokeCap.Round)
    drawLine(color, Offset(x, bottom), Offset(x + 12f, bottom - 16f), 5f, StrokeCap.Round)
}
