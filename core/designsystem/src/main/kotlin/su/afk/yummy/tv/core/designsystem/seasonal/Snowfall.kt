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

private const val FlakeCount = 26
private const val SEED = 20260101
private const val NanosPerSecond = 1_000_000_000f

/** Доля снежинок, которые «тают» в полёте, не долетев до низа (в каждом новом падении выбирается заново). */
private const val DissolveChance = 0.4f
private const val DissolveFadeLength = 0.2f

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
        flakes.forEachIndexed { index, flake ->
            val radius = flake.radiusDp.dp.toPx()
            val travelled = flake.offset + t * flake.speed
            val progress = travelled % 1f
            val y = progress * (size.height + radius * 4f) - radius * 2f
            val x = flake.x * size.width + sin(t * flake.sway + flake.phase) * flake.swayDp.dp.toPx()
            val alpha = flake.alpha * dissolveFactor(index, travelled.toInt(), progress)
            if (alpha > 0f) drawCircle(Color.White.copy(alpha = alpha), radius, Offset(x, y))
        }
    }
}

/**
 * Множитель прозрачности: часть снежинок в конкретном падении плавно растворяется на случайной высоте.
 * Решение детерминировано по (снежинка, номер падения), без состояния и аллокаций.
 */
private fun dissolveFactor(flakeIndex: Int, fallIndex: Int, progress: Float): Float {
    val hash = (flakeIndex * 73856093) xor (fallIndex * 19349663)
    val chance = (hash and 0xFFFF) / 65535f
    if (chance >= DissolveChance) return 1f
    val endsAt = 0.35f + ((hash ushr 16) and 0xFFFF) / 65535f * 0.5f
    val start = endsAt - DissolveFadeLength
    return (1f - (progress - start) / DissolveFadeLength).coerceIn(0f, 1f)
}
