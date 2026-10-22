package su.afk.yummy.tv.core.designsystem.seasonal

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.runtime.State
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.core.designsystem.skeleton.rememberAnimationsEnabled

private const val SwingMillis = 900
private const val SwingDegrees = 12f

/** Индикатор загрузки на Хэллоуин: покачивающаяся тыква вместо кольца. */
@Composable
fun PumpkinLoader(modifier: Modifier = Modifier) {
    val swing: State<Float> = if (rememberAnimationsEnabled()) {
        rememberInfiniteTransition(label = "pumpkin").animateFloat(
            initialValue = -1f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                tween(SwingMillis, easing = FastOutSlowInEasing),
                RepeatMode.Reverse,
            ),
            label = "pumpkinSwing",
        )
    } else {
        remember { mutableFloatStateOf(0f) }
    }
    Canvas(modifier.defaultMinSize(48.dp, 48.dp)) {
        val base = Offset(size.width / 2f, size.height * 0.95f)
        rotate(degrees = swing.value * SwingDegrees, pivot = base) {
            drawPumpkin(base, size.minDimension * 0.7f)
        }
    }
}
