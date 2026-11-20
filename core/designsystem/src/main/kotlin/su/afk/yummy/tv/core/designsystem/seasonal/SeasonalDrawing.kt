package su.afk.yummy.tv.core.designsystem.seasonal

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.pow
import kotlin.math.PI

private val PumpkinColor = Color(0xFFFF7A1A)
private val PumpkinDark = Color(0xFFD95F00)
private val StemColor = Color(0xFF3E7D32)
private val FaceColor = Color(0xFF2A1400)
private val WebColor = Color(0xF2EEF0FA)
private val WebShadow = Color(0x80000000)
private val SpiderColor = Color(0xFF1E1E24)
private val SpiderEyes = Color(0xFFFF3B3B)
private val SnowColor = Color(0xF2FFFFFF)
private val BerryRed = Color(0xFFC62828)
private val BerryShine = Color(0xFFFF8A80)
private val BowDark = Color(0xFFB71C1C)
private val BowShade = Color(0xFF9E1B1B)
private val BowKnot = Color(0xFFE53935)
private val WreathBase = Color(0xFF14452A)
private const val WreathUnit = 0.0078f
private const val WreathRadius = 45f
private val BowLoopOuter = listOf(8f to -17f, 27f to -14f, 24f to 1f, 22f to 10f, 9f to 6f)
private val BowLoopInner = listOf(7f to -9f, 16f to -8f, 17f to -1f, 14f to 3f, 7f to 3f)
private val WreathBerryAngles = listOf(-60, 15, 100, 190, 235)
private val WreathBerrySpots = listOf(0f to 0.0, 3.8f to 0.07, -3.4f to 0.06)
private val WreathLayers = listOf(
    WreathLayer(30, 0.0, -0.55, 2.5f, 19f, 5.2f, listOf(Color(0xFF1F6B34), Color(0xFF27793C))),
    WreathLayer(30, PI / 30.0, 0.55, -2.5f, 17f, 4.8f, listOf(Color(0xFF2E8B45), Color(0xFF37984D), Color(0xFF43A55A))),
    WreathLayer(24, PI / 24.0, -0.35, 0f, 13f, 3.6f, listOf(Color(0xFF5CBF73), Color(0xFF4FB068))),
)

/** Паутина из угла [corner]: [direction] 1 — вправо от угла, -1 — влево. */
internal fun DrawScope.drawCornerWeb(corner: Offset, direction: Float, radius: Float) {
    val width = 1.3.dp.toPx()
    val rays = listOf(0f, 0.33f, 0.66f, 1f).map { r ->
        Offset(corner.x + direction * radius * (1f - r), corner.y + radius * r)
    }
    val rings = listOf(0.3f, 0.55f, 0.8f, 1f).map { ring ->
        Path().apply {
            moveTo(corner.x + direction * radius * ring, corner.y)
            quadraticTo(
                corner.x + direction * radius * ring * 0.35f,
                corner.y + radius * ring * 0.35f,
                corner.x,
                corner.y + radius * ring,
            )
        }
    }
    // Тёмная подложка под светлой нитью: паутина читается и на светлых постерах.
    for ((color, strokeWidth) in listOf(WebShadow to width * 2.2f, WebColor to width)) {
        rays.forEach { end -> drawLine(color, corner, end, strokeWidth = strokeWidth) }
        rings.forEach { path -> drawPath(path, color, style = Stroke(width = strokeWidth)) }
    }
}

/** Тыква: [base] — середина низа, [size] — высота, [alpha] — прозрачность всей тыквы. */
internal fun DrawScope.drawPumpkin(base: Offset, size: Float, alpha: Float = 1f) {
    val width = size * 1.25f
    val top = base.y - size
    val left = base.x - width / 2f
    drawOval(PumpkinDark.copy(alpha = alpha), Offset(left, top), Size(width, size))
    drawOval(PumpkinColor.copy(alpha = alpha), Offset(left + width * 0.2f, top), Size(width * 0.6f, size))
    drawRect(StemColor.copy(alpha = alpha), Offset(base.x - size * 0.06f, top - size * 0.22f), Size(size * 0.12f, size * 0.26f))
    val eye = size * 0.14f
    drawCircle(FaceColor.copy(alpha = alpha), eye, Offset(base.x - width * 0.18f, top + size * 0.4f))
    drawCircle(FaceColor.copy(alpha = alpha), eye, Offset(base.x + width * 0.18f, top + size * 0.4f))
    val smile = Path().apply {
        moveTo(base.x - width * 0.2f, top + size * 0.65f)
        quadraticTo(base.x, top + size * 0.85f, base.x + width * 0.2f, top + size * 0.65f)
    }
    drawPath(smile, FaceColor.copy(alpha = alpha), style = Stroke(width = size * 0.08f))
}

/**
 * Снежный сугроб вдоль верхней кромки: к краям плавно сходит на нет, нижняя линия волнистая.
 * [inset] отступает от краёв (скруглённые углы кнопок).
 */
internal fun DrawScope.drawSnowCap(capHeight: Float, inset: Float = 0f) {
    val width = size.width - inset * 2f
    if (width <= 0f) return
    val step = (capHeight * 0.75f).coerceAtLeast(1f)
    val samples = (width / step).toInt().coerceAtLeast(2)
    val waveLength = capHeight * 5f
    val path = Path().apply {
        moveTo(inset, 0f)
        for (i in 0..samples) {
            val t = i / samples.toFloat()
            val x = inset + width * t
            // Огибающая: ноль на концах, чтобы снег не обрывался стенкой.
            val envelope = sin(PI * t).toFloat().coerceAtLeast(0f).pow(0.7f)
            val wobble = 0.7f + 0.2f * sin(x / waveLength * 2f * PI.toFloat()) +
                0.1f * sin(x / (waveLength * 0.37f) * 2f * PI.toFloat() + 1.3f)
            lineTo(x, capHeight * 1.5f * envelope * wobble)
        }
        lineTo(inset + width, 0f)
        close()
    }
    drawPath(path, SnowColor)
}

/**
 * Новогодний венок по краю круглой аватарки размера [size]: два слоя заострённых хвойных листьев, кластеры ягод
 * и бант внизу. Рисунок целиком лежит в квадрате [size], кроме хвостов банта.
 */
internal fun DrawScope.drawNewYearWreath(size: Float) {
    val k = size * WreathUnit
    val center = Offset(size / 2f, size / 2f)
    val ring = WreathRadius * k
    drawCircle(WreathBase, ring, center, style = Stroke(width = 9f * k))
    for (layer in WreathLayers) {
        for (i in 0 until layer.count) {
            val angle = 2.0 * PI * i / layer.count + layer.offset
            val radius = ring + layer.radius * k
            val base = Offset(
                center.x + radius * cos(angle).toFloat(),
                center.y + radius * sin(angle).toFloat(),
            )
            drawLeaf(base, (angle + PI / 2.0 + layer.tilt).toFloat(), layer.length * k, layer.width * k, layer.colors[i % layer.colors.size])
        }
    }
    for (degrees in WreathBerryAngles) {
        val angle = Math.toRadians(degrees.toDouble())
        for ((index, spot) in WreathBerrySpots.withIndex()) {
            val berryAngle = angle + spot.second * (if (index == 2) -1.0 else 1.0)
            val berryRadius = ring + spot.first * k
            val at = Offset(
                center.x + berryRadius * cos(berryAngle).toFloat(),
                center.y + berryRadius * sin(berryAngle).toFloat(),
            )
            drawCircle(BerryRed, 3.3f * k, at)
            drawCircle(BerryShine, 1f * k, Offset(at.x - 0.9f * k, at.y - 0.9f * k))
        }
    }
    drawWreathBow(Offset(center.x, center.y + ring + 2f * k), k)
}

private fun DrawScope.drawLeaf(base: Offset, angle: Float, length: Float, width: Float, color: Color) {
    val cosA = cos(angle)
    val sinA = sin(angle)
    fun point(x: Float, y: Float) = Offset(base.x + x * cosA - y * sinA, base.y + x * sinA + y * cosA)
    val path = Path().apply {
        moveTo(base.x, base.y)
        val c1 = point(length * 0.5f, -width)
        val tip = point(length, 0f)
        quadraticTo(c1.x, c1.y, tip.x, tip.y)
        val c2 = point(length * 0.5f, width)
        quadraticTo(c2.x, c2.y, base.x, base.y)
        close()
    }
    drawPath(path, color)
}

/** Бант венка с узлом в [knot]; [k] — масштаб относительно координат иконки. */
private fun DrawScope.drawWreathBow(knot: Offset, k: Float) {
    fun at(x: Float, y: Float) = Offset(knot.x + x * k, knot.y + y * k)
    for (side in listOf(-1f, 1f)) {
        val tail = Path().apply {
            val start = at(side * 2f, 2f)
            moveTo(start.x, start.y)
            for ((x, y) in listOf(12f to 13f, 9f to 12f, 7f to 15f, 0f to 6f)) {
                val p = at(side * x, y)
                lineTo(p.x, p.y)
            }
            close()
        }
        drawPath(tail, BowDark)
    }
    for (side in listOf(-1f, 1f)) {
        drawPath(bowLoop(::at, side, BowLoopOuter), BerryRed)
        drawPath(bowLoop(::at, side, BowLoopInner), BowShade)
    }
    drawCircle(BowKnot, 4.8f * k, knot)
    drawCircle(BerryShine, 1.6f * k, at(-1f, -1f))
}

/** Петля банта: координаты [points] — две контрольные точки и конец первой кривой, две контрольные второй. */
private fun bowLoop(at: (Float, Float) -> Offset, side: Float, points: List<Pair<Float, Float>>): Path = Path().apply {
    fun p(index: Int) = at(side * points[index].first, points[index].second)
    val start = at(0f, 0f)
    moveTo(start.x, start.y)
    cubicTo(p(0).x, p(0).y, p(1).x, p(1).y, p(2).x, p(2).y)
    cubicTo(p(3).x, p(3).y, p(4).x, p(4).y, start.x, start.y)
    close()
}

private class WreathLayer(
    val count: Int,
    val offset: Double,
    val tilt: Double,
    val radius: Float,
    val length: Float,
    val width: Float,
    val colors: List<Color>,
)

/** Паук с телом радиуса [radius] в [center]. */
internal fun DrawScope.drawSpider(center: Offset, radius: Float) {
    val leg = (0.9.dp.toPx()).coerceAtMost(radius * 0.35f)
    for (side in listOf(-1f, 1f)) {
        for (i in 0 until 3) {
            val y = center.y + (i - 1) * radius * 0.5f
            drawLine(
                SpiderColor,
                center,
                Offset(center.x + side * radius * 1.7f, y + radius * (0.4f - i * 0.45f)),
                strokeWidth = leg,
            )
        }
    }
    drawCircle(SpiderColor, radius, center)
    drawCircle(SpiderEyes, radius * 0.16f, Offset(center.x - radius * 0.3f, center.y + radius * 0.35f))
    drawCircle(SpiderEyes, radius * 0.16f, Offset(center.x + radius * 0.3f, center.y + radius * 0.35f))
}

/** Рисует [block] в отдельном слое с общей прозрачностью [alpha]: для составных рисунков. */
internal inline fun DrawScope.withLayerAlpha(alpha: Float, block: DrawScope.() -> Unit) {
    drawIntoCanvas { canvas ->
        canvas.saveLayer(androidx.compose.ui.geometry.Rect(Offset.Zero, size), Paint().apply { this.alpha = alpha })
        block()
        canvas.restore()
    }
}
