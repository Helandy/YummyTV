package su.afk.yummy.tv.core.designsystem.seasonal

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.sin

private val GarlandHeight = 16.dp
private val SegmentWidth = 84.dp
internal val WireColor = Color(0xFF3B3B3B)
internal val BulbColors = listOf(
    Color(0xFFFF4D4D),
    Color(0xFFFFC83D),
    Color(0xFF4DD66B),
    Color(0xFF4DA6FF),
)
internal const val TwinkleMillis = 2400
private const val BulbsPerSegment = 3

/** Новогодняя гирлянда: провисающий провод с мерцающими лампочками. Касания не перехватывает. */
@Composable
fun NewYearGarland(modifier: Modifier = Modifier, height: Dp = GarlandHeight) {
    val phase by rememberInfiniteTransition(label = "garland").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(TwinkleMillis, easing = LinearEasing), RepeatMode.Restart),
        label = "garlandPhase",
    )
    Canvas(modifier.fillMaxWidth().height(height)) {
        drawGarland(phase)
    }
}

private fun DrawScope.drawGarland(phase: Float) {
    val segmentWidth = SegmentWidth.toPx()
    val sag = size.height * 0.45f
    val bulbRadius = 2.6.dp.toPx()
    val segments = (size.width / segmentWidth).toInt().coerceAtLeast(1)
    val step = size.width / segments
    var bulbIndex = 0
    for (segment in 0 until segments) {
        val x0 = segment * step
        val x1 = x0 + step
        val control = Offset((x0 + x1) / 2f, sag * 2f)
        val wire = Path().apply {
            moveTo(x0, 0f)
            quadraticTo(control.x, control.y, x1, 0f)
        }
        drawPath(wire, WireColor, style = Stroke(width = 1.dp.toPx()))
        for (i in 1..BulbsPerSegment) {
            val t = i / (BulbsPerSegment + 1f)
            val point = quadraticPoint(Offset(x0, 0f), control, Offset(x1, 0f), t)
            val color = BulbColors[bulbIndex % BulbColors.size]
            val twinkle = 0.55f + 0.45f * sin(2.0 * PI * (phase + bulbIndex * 0.25f)).toFloat()
            drawBulb(point, bulbRadius, color, twinkle)
            bulbIndex++
        }
    }
}

private fun DrawScope.drawBulb(anchor: Offset, radius: Float, color: Color, alpha: Float) {
    val center = Offset(anchor.x, anchor.y + radius * 1.8f)
    drawRect(WireColor, topLeft = Offset(anchor.x - radius * 0.5f, anchor.y), size = Size(radius, radius * 0.9f))
    drawCircle(color.copy(alpha = alpha * 0.25f), radius * 2.1f, center)
    drawCircle(color.copy(alpha = alpha), radius, center)
}

internal fun quadraticPoint(p0: Offset, p1: Offset, p2: Offset, t: Float): Offset {
    val u = 1f - t
    return Offset(
        u * u * p0.x + 2f * u * t * p1.x + t * t * p2.x,
        u * u * p0.y + 2f * u * t * p1.y + t * t * p2.y,
    )
}
