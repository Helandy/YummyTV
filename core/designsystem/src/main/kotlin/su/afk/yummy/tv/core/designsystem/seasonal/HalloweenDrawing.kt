package su.afk.yummy.tv.core.designsystem.seasonal

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private val WitchCone = Color(0xFF6A3CC4)
private val WitchBrim = Color(0xFF4A2A94)
private val WitchBand = Color(0xFFFFD54A)
private val GhostColor = Color(0xFFF4F6FF)
private val GhostFace = Color(0xFF2A2440)
private val StoneColor = Color(0xFF9A9FB3)
private val StoneDark = Color(0xFF6C7087)
private val TreeColor = Color(0xFF5A4A78)
private val PotColor = Color(0xFF2E2E3C)
private val PotLight = Color(0xFF4A4A60)
private val BrewColor = Color(0xFF6BE04A)
private val BrewLight = Color(0xFFB5F27A)

/** Шляпа ведьмы на круглой аватарке размера [size] (от левого верхнего угла аватарки). */
internal fun DrawScope.drawWitchHat(size: Float) {
    // Конус невысокий: у аватарок верх часто обрезан родителем, поэтому вершина остаётся внутри.
    rotate(degrees = 18f, pivot = Offset(size * 0.62f, size * 0.2f)) {
        val baseY = size * 0.34f
        val cone = Path().apply {
            moveTo(size * 0.28f, baseY)
            lineTo(size * 0.58f, size * 0.05f)
            quadraticTo(size * 0.66f, -size * 0.03f, size * 0.76f, size * 0.1f)
            lineTo(size * 0.9f, baseY)
            close()
        }
        drawPath(cone, WitchCone)
        drawRect(WitchBand, Offset(size * 0.29f, baseY - size * 0.1f), Size(size * 0.6f, size * 0.1f))
        drawRoundRect(
            WitchBrim,
            Offset(size * 0.05f, baseY - size * 0.02f),
            Size(size * 1.0f, size * 0.12f),
            cornerRadius = CornerRadius(size * 0.06f),
        )
    }
}

/** Призрак: [center] — середина тела, [size] — высота; [alpha] — общая прозрачность. */
internal fun DrawScope.drawGhost(center: Offset, size: Float, alpha: Float) {
    val width = size * 0.78f
    val left = center.x - width / 2f
    val right = center.x + width / 2f
    val top = center.y - size / 2f
    val bottom = center.y + size / 2f
    val body = Path().apply {
        moveTo(left, bottom)
        lineTo(left, top + width / 2f)
        arcTo(Rect(left, top, right, top + width), 180f, 180f, false)
        lineTo(right, bottom)
        val scallop = width / 3f
        var x = right
        repeat(3) {
            quadraticTo(x - scallop * 0.5f, bottom - size * 0.16f, x - scallop, bottom)
            x -= scallop
        }
        close()
    }
    drawPath(body, GhostColor.copy(alpha = alpha))
    val eye = size * 0.06f
    drawOval(
        GhostFace.copy(alpha = alpha),
        Offset(center.x - width * 0.22f - eye, center.y - size * 0.18f),
        Size(eye * 2f, eye * 2.6f),
    )
    drawOval(
        GhostFace.copy(alpha = alpha),
        Offset(center.x + width * 0.22f - eye, center.y - size * 0.18f),
        Size(eye * 2f, eye * 2.6f),
    )
    drawOval(
        GhostFace.copy(alpha = alpha),
        Offset(center.x - eye * 0.8f, center.y + size * 0.02f),
        Size(eye * 1.6f, eye * 2.2f),
    )
}

/** Надгробие: [base] — середина низа, [height] — высота; [cross] — крест вместо скруглённого верха. */
internal fun DrawScope.drawTombstone(base: Offset, height: Float, cross: Boolean = false) {
    if (cross) {
        val bar = height * 0.16f
        drawRoundRect(StoneColor, Offset(base.x - bar / 2f, base.y - height), Size(bar, height), CornerRadius(bar / 3f))
        drawRoundRect(
            StoneColor,
            Offset(base.x - height * 0.3f, base.y - height * 0.72f),
            Size(height * 0.6f, bar),
            CornerRadius(bar / 3f),
        )
        return
    }
    val width = height * 0.7f
    val left = base.x - width / 2f
    val top = base.y - height
    val stone = Path().apply {
        moveTo(left, base.y)
        lineTo(left, top + width / 2f)
        arcTo(Rect(left, top, left + width, top + width), 180f, 180f, false)
        lineTo(left + width, base.y)
        close()
    }
    drawPath(stone, StoneColor)
    drawPath(stone, StoneDark, style = Stroke(width = height * 0.06f))
    val bar = height * 0.07f
    drawRect(StoneDark, Offset(base.x - width * 0.22f, top + height * 0.34f), Size(width * 0.44f, bar))
    drawRect(StoneDark, Offset(base.x - width * 0.22f, top + height * 0.5f), Size(width * 0.44f, bar))
}

/** Голое дерево: [base] — середина корня, [height] — высота; ветви разветвляются детерминированно. */
internal fun DrawScope.drawDeadTree(base: Offset, height: Float) {
    fun branch(start: Offset, angleDeg: Float, length: Float, widthPx: Float, depth: Int) {
        val rad = angleDeg * PI.toFloat() / 180f
        val end = Offset(start.x + cos(rad) * length, start.y - sin(rad) * length)
        drawLine(TreeColor, start, end, strokeWidth = widthPx, cap = StrokeCap.Round)
        if (depth <= 0) return
        branch(end, angleDeg + 32f, length * 0.68f, widthPx * 0.68f, depth - 1)
        branch(end, angleDeg - 28f, length * 0.72f, widthPx * 0.68f, depth - 1)
    }
    branch(base, 90f, height * 0.42f, height * 0.09f, 0)
    val fork = Offset(base.x, base.y - height * 0.42f)
    branch(fork, 125f, height * 0.4f, height * 0.06f, 2)
    branch(fork, 62f, height * 0.46f, height * 0.06f, 2)
    branch(fork, 92f, height * 0.3f, height * 0.05f, 1)
}

/** Ведьмин котёл с булькающим зельем: [base] — середина низа, [size] — высота; [phase] 0..1 — пузыри. */
internal fun DrawScope.drawCauldron(base: Offset, size: Float, phase: Float) {
    val width = size * 1.25f
    val left = base.x - width / 2f
    val top = base.y - size
    val rimHeight = size * 0.26f
    drawRoundRect(PotColor, Offset(left + width * 0.12f, base.y - size * 0.18f), Size(width * 0.14f, size * 0.18f), CornerRadius(size * 0.04f))
    drawRoundRect(PotColor, Offset(left + width * 0.74f, base.y - size * 0.18f), Size(width * 0.14f, size * 0.18f), CornerRadius(size * 0.04f))
    val body = Path().apply {
        moveTo(left, top + rimHeight / 2f)
        quadraticTo(left - width * 0.04f, base.y - size * 0.1f, left + width * 0.2f, base.y - size * 0.06f)
        lineTo(left + width * 0.8f, base.y - size * 0.06f)
        quadraticTo(left + width * 1.04f, base.y - size * 0.1f, left + width, top + rimHeight / 2f)
        close()
    }
    drawPath(body, PotColor)
    drawOval(PotLight, Offset(left, top), Size(width, rimHeight))
    drawOval(BrewColor, Offset(left + width * 0.06f, top + rimHeight * 0.14f), Size(width * 0.88f, rimHeight * 0.72f))
    drawOval(BrewLight.copy(alpha = 0.6f), Offset(left + width * 0.2f, top + rimHeight * 0.26f), Size(width * 0.3f, rimHeight * 0.26f))
    for (i in 0 until 3) {
        val t = (phase + i / 3f) % 1f
        val x = base.x + (i - 1) * width * 0.22f + sin(t * 2f * PI.toFloat() + i) * size * 0.05f
        val y = top + rimHeight * 0.5f - t * size * 0.55f
        drawCircle(BrewLight.copy(alpha = 1f - t), size * (0.06f + 0.04f * (1f - t)), Offset(x, y))
    }
}
