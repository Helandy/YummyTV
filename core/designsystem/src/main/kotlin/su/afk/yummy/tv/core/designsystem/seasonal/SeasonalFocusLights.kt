package su.afk.yummy.tv.core.designsystem.seasonal

import su.afk.yummy.tv.core.model.seasonal.SeasonalTheme
import androidx.compose.animation.core.LinearEasing
import androidx.compose.runtime.State
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.core.designsystem.skeleton.rememberAnimationsEnabled
import kotlin.math.PI
import kotlin.math.sin

private const val FocusTwinkleMillis = 2200
private val LightColors = listOf(
    Color(0xFFFF4D4D),
    Color(0xFFFFC83D),
    Color(0xFF4DD66B),
    Color(0xFF4DA6FF),
)

/**
 * Новогодняя рамка фокуса: по контуру [shape] бегут мерцающие лампочки. Вне Нового года и без
 * фокуса не делает ничего. Ставить до `border`, чтобы лампочки легли поверх рамки.
 */
@Composable
fun Modifier.seasonalFocusLights(focused: Boolean, shape: Shape): Modifier {
    val theme = rememberSeasonalTheme()
    val animationsEnabled = rememberAnimationsEnabled()
    if (!focused || theme != SeasonalTheme.NEW_YEAR) return this
    val phase: State<Float> = if (animationsEnabled) {
        rememberInfiniteTransition(label = "focusLights").animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(FocusTwinkleMillis, easing = LinearEasing)),
            label = "focusLightsPhase",
        )
    } else {
        remember { mutableFloatStateOf(0f) }
    }
    return drawWithCache {
        val path = Path().apply {
            when (val outline = shape.createOutline(size, layoutDirection, this@drawWithCache)) {
                is Outline.Rectangle -> addRect(outline.rect)
                is Outline.Rounded -> addRoundRect(outline.roundRect)
                is Outline.Generic -> addPath(outline.path)
            }
        }
        val measure = PathMeasure().apply { setPath(path, true) }
        val length = measure.length
        val count = (length / 14.dp.toPx()).toInt().coerceAtLeast(6)
        val points: List<Offset> = if (length > 0f) {
            List(count) { measure.getPosition(length * it / count) }
        } else {
            emptyList()
        }
        val radius = 2.4.dp.toPx()
        onDrawWithContent {
            drawContent()
            points.forEachIndexed { index, point ->
                val color = LightColors[index % LightColors.size]
                val twinkle = 0.6f + 0.4f * sin(2.0 * PI * (phase.value + index * 0.25f)).toFloat()
                drawCircle(color.copy(alpha = twinkle * 0.3f), radius * 2f, point)
                drawCircle(color.copy(alpha = twinkle), radius, point)
            }
        }
    }
}
