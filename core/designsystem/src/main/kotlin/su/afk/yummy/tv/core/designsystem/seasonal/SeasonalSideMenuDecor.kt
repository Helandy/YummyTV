package su.afk.yummy.tv.core.designsystem.seasonal

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.core.designsystem.skeleton.rememberAnimationsEnabled
import su.afk.yummy.tv.core.model.seasonal.SeasonalTheme
import kotlin.math.PI
import kotlin.math.sin

private val SegmentHeight = 84.dp
private const val BulbsPerSegment = 3

/**
 * Праздничное оформление боковой панели ТВ: вертикальная гирлянда вдоль правой кромки (Новый год)
 * либо паутина в верхнем и нижнем углу с пауком на нити (Хэллоуин). Рисуется поверх панели,
 * фокус и касания не затрагивает.
 */
@Composable
fun Modifier.seasonalSideMenuDecor(
    topOffset: Dp = 0.dp,
    bottomOffset: Dp? = null,
    expandProgress: () -> Float = { 0f },
): Modifier {
    val theme = rememberSeasonalTheme()
    val animationsEnabled = rememberAnimationsEnabled()
    val phase: State<Float> = if (theme == SeasonalTheme.NEW_YEAR && animationsEnabled) {
        rememberInfiniteTransition(label = "sideGarland").animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(TwinkleMillis, easing = LinearEasing)),
            label = "sideGarlandPhase",
        )
    } else {
        remember { mutableFloatStateOf(0f) }
    }
    return when (theme) {
        SeasonalTheme.NONE -> this
        SeasonalTheme.NEW_YEAR -> drawWithContent {
            drawContent()
            drawVerticalGarland(phase.value)
        }
        SeasonalTheme.HALLOWEEN -> drawWithContent {
            drawContent()
            drawHalloweenSideMenu(expandProgress(), topOffset.toPx(), bottomOffset?.toPx())
        }
    }
}

private fun DrawScope.drawVerticalGarland(phase: Float) {
    val segmentHeight = SegmentHeight.toPx()
    val sag = 14.dp.toPx()
    val radius = 2.6.dp.toPx()
    val x = size.width
    val segments = (size.height / segmentHeight).toInt().coerceAtLeast(1)
    val step = size.height / segments
    var bulbIndex = 0
    for (segment in 0 until segments) {
        val y0 = segment * step
        val y1 = y0 + step
        val control = Offset(x - sag * 2f, (y0 + y1) / 2f)
        val wire = Path().apply {
            moveTo(x, y0)
            quadraticTo(control.x, control.y, x, y1)
        }
        drawPath(wire, WireColor, style = Stroke(width = 1.dp.toPx()))
        for (i in 1..BulbsPerSegment) {
            val point = quadraticPoint(Offset(x, y0), control, Offset(x, y1), i / (BulbsPerSegment + 1f))
            val color = BulbColors[bulbIndex % BulbColors.size]
            val twinkle = 0.55f + 0.45f * sin(2.0 * PI * (phase + bulbIndex * 0.25f)).toFloat()
            val center = Offset(point.x - radius * 1.8f, point.y)
            drawRect(
                WireColor,
                Offset(point.x - radius * 0.9f, point.y - radius * 0.5f),
                androidx.compose.ui.geometry.Size(radius * 0.9f, radius),
            )
            drawCircle(color.copy(alpha = twinkle * 0.25f), radius * 2.1f, center)
            drawCircle(color.copy(alpha = twinkle), radius, center)
            bulbIndex++
        }
    }
}

private fun DrawScope.drawHalloweenSideMenu(expandProgress: Float, topOffset: Float, pumpkinInset: Float?) {
    val web = minOf(size.width * 0.6f, 44.dp.toPx())
    drawCornerWeb(Offset(size.width, topOffset), -1f, web)
    if (pumpkinInset != null) {
        // Вместо нижней паутины — тыква (рейка на фолде, где снизу свободное место).
        drawPumpkin(Offset(size.width / 2f, size.height - pumpkinInset - 12.dp.toPx()), 28.dp.toPx())
    } else {
        // Нижняя паутина — зеркало верхней по горизонтальной оси.
        scale(scaleX = 1f, scaleY = -1f, pivot = Offset(size.width / 2f, size.height / 2f)) {
            drawCornerWeb(Offset(size.width, 0f), -1f, web)
        }
    }
    val threadX = size.width - 12.dp.toPx()
    val threadLength = 46.dp.toPx()
    drawLine(
        androidx.compose.ui.graphics.Color(0x99B8B8C8),
        Offset(threadX, topOffset),
        Offset(threadX, topOffset + threadLength),
        strokeWidth = 0.8.dp.toPx(),
    )
    drawSpider(Offset(threadX, topOffset + threadLength + 4.dp.toPx()), 4.dp.toPx())
    // Тыквы, надгробие и дерево живут в свободной части раскрытой панели и проявляются вместе с ней.
    if (expandProgress > 0f) {
        val bottom = size.height - 16.dp.toPx()
        drawPumpkin(Offset(size.width - 38.dp.toPx(), bottom), 30.dp.toPx(), expandProgress)
        drawPumpkin(Offset(size.width - 28.dp.toPx(), bottom - 44.dp.toPx()), 16.dp.toPx(), expandProgress)
        withLayerAlpha(expandProgress * 0.85f) {
            drawDeadTree(Offset(size.width - 20.dp.toPx(), bottom - 60.dp.toPx()), 92.dp.toPx())
            drawTombstone(Offset(size.width - 78.dp.toPx(), bottom), 24.dp.toPx())
        }
    }
}
