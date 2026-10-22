package su.afk.yummy.tv.core.designsystem.seasonal

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlin.math.sin
import kotlin.random.Random

private const val FlakeCount = 36
private const val SEED = 20260101
private const val NanosPerSecond = 1_000_000_000f

private class Flake(
    val x: Float,
    val offset: Float,
    val speed: Float,
    val radiusDp: Float,
    val alpha: Float,
    val sway: Float,
    val phase: Float,
    val swayDp: Float,
)

/** Лёгкий снегопад: один Canvas, время читается только в draw — рекомпозиций нет. */
@Composable
internal fun Snowfall(modifier: Modifier = Modifier) {
    val flakes = remember {
        val random = Random(SEED)
        List(FlakeCount) {
            Flake(
                x = random.nextFloat(),
                offset = random.nextFloat(),
                speed = 0.05f + random.nextFloat() * 0.07f,
                radiusDp = 1.2f + random.nextFloat() * 1.8f,
                alpha = 0.45f + random.nextFloat() * 0.45f,
                sway = 0.6f + random.nextFloat() * 0.9f,
                phase = random.nextFloat() * 6.28f,
                swayDp = 6f + random.nextFloat() * 10f,
            )
        }
    }
    val nanos = remember { mutableLongStateOf(0L) }
    LaunchedEffect(Unit) {
        while (true) withFrameNanos { nanos.longValue = it }
    }
    Canvas(modifier.fillMaxSize()) {
        val t = nanos.longValue / NanosPerSecond
        flakes.forEach { flake ->
            val radius = flake.radiusDp.dp.toPx()
            val progress = (flake.offset + t * flake.speed) % 1f
            val y = progress * (size.height + radius * 4f) - radius * 2f
            val x = flake.x * size.width + sin(t * flake.sway + flake.phase) * flake.swayDp.dp.toPx()
            drawCircle(Color.White.copy(alpha = flake.alpha), radius, Offset(x, y))
        }
    }
}
