package su.afk.yummy.tv.core.designsystem.seasonal

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp

private val PumpkinColor = Color(0xFFFF7A1A)
private val PumpkinDark = Color(0xFFD95F00)
private val StemColor = Color(0xFF3E7D32)
private val FaceColor = Color(0xFF2A1400)
private val WebColor = Color(0xF2EEF0FA)
private val WebShadow = Color(0x80000000)
private val SpiderColor = Color(0xFF1E1E24)
private val SpiderEyes = Color(0xFFFF3B3B)
private val SnowColor = Color(0xF2FFFFFF)
private val HatRed = Color(0xFFD62828)
private val HatWhite = Color(0xFFFFFFFF)

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

/** Снежная шапка вдоль верхней кромки: [inset] отступает от краёв (скруглённые углы кнопок). */
internal fun DrawScope.drawSnowCap(capHeight: Float, inset: Float = 0f) {
    val width = size.width - inset * 2f
    if (width <= 0f) return
    drawRect(SnowColor, Offset(inset, 0f), Size(width, capHeight))
    val bumps = (width / (capHeight * 2.2f)).toInt().coerceAtLeast(1)
    val bumpStep = width / bumps
    for (i in 0 until bumps) {
        val radius = capHeight * (0.5f + 0.35f * ((i * 7) % 5) / 4f)
        drawCircle(SnowColor, radius, Offset(inset + bumpStep * (i + 0.5f), capHeight))
    }
}

/** Шапка Деда Мороза на круглой аватарке размера [size] (от левого верхнего угла аватарки). */
internal fun DrawScope.drawSantaHat(size: Float) {
    rotate(degrees = 18f, pivot = Offset(size * 0.62f, size * 0.12f)) {
        val baseY = size * 0.2f
        val left = size * 0.18f
        val right = size * 1.04f
        val body = Path().apply {
            moveTo(left, baseY)
            quadraticTo(size * 0.45f, -size * 0.34f, size * 0.98f, -size * 0.12f)
            lineTo(right, baseY)
            close()
        }
        drawPath(body, HatRed)
        drawRoundRect(
            HatWhite,
            Offset(left - size * 0.04f, baseY - size * 0.07f),
            Size(right - left + size * 0.08f, size * 0.15f),
            cornerRadius = CornerRadius(size * 0.075f),
        )
        drawCircle(HatWhite, size * 0.1f, Offset(size * 0.98f, -size * 0.12f))
    }
}

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
