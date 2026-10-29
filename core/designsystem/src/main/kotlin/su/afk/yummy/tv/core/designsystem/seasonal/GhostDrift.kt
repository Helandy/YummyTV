package su.afk.yummy.tv.core.designsystem.seasonal

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

private const val MinDriftPauseMillis = 180_000L
private const val MaxDriftPauseMillis = 600_000L
private const val DriftSeconds = 16f
private const val NanosPerSecond = 1_000_000_000f

/**
 * Редко (раз в 3–10 минут, случайно) над экраном плывёт призрак: полупрозрачный, покачивается.
 * Пока он не летит, кадры не запрашиваются: цикл анимации живёт только на время полёта.
 */
@Composable
internal fun GhostDrift(modifier: Modifier = Modifier) {
    val progress = remember { mutableFloatStateOf(-1f) }
    val seconds = remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(Random.nextLong(MinDriftPauseMillis, MaxDriftPauseMillis + 1))
            val start = withFrameNanos { it }
            do {
                val now = withFrameNanos { it }
                seconds.floatValue = (now - start) / NanosPerSecond
                progress.floatValue = seconds.floatValue / DriftSeconds
            } while (progress.floatValue < 1f)
            progress.floatValue = -1f
        }
    }
    Canvas(modifier.fillMaxSize()) {
        val p = progress.floatValue
        if (p < 0f) return@Canvas
        val alpha = sin(p.coerceIn(0f, 1f) * PI.toFloat()).coerceIn(0f, 1f) * 0.6f
        val x = size.width * (0.15f + 0.7f * p) + sin(seconds.floatValue * 1.6f) * 18.dp.toPx()
        val y = size.height * (0.78f - 0.5f * p) + sin(seconds.floatValue * 2.1f) * 8.dp.toPx()
        drawGhost(Offset(x, y), 64.dp.toPx(), alpha)
    }
}
