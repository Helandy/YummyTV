package su.afk.yummy.tv.core.designsystem.seasonal

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import su.afk.yummy.tv.core.designsystem.skeleton.rememberAnimationsEnabled
import su.afk.yummy.tv.core.model.seasonal.SeasonalTheme

private val ThreadColor = Color(0x99B8B8C8)
private const val SpiderCycleMillis = 20_000
private const val DropEnd = 0.05f
private const val HoldEnd = 0.16f
private const val ClimbEnd = 0.21f
private const val NanosPerMilli = 1_000_000f

/**
 * Общая фаза 0..[ClimbEnd] (цикл 20 с) для хэллоуинских «оживалок»: паук спускается на нити.
 * Без Хэллоуина и при «Убрать анимации» стоит на нуле.
 */
@Composable
internal fun rememberSpiderPhase(): State<Float> {
    val theme = rememberSeasonalTheme()
    val animationsEnabled = rememberAnimationsEnabled()
    val phase = remember { mutableFloatStateOf(0f) }
    if (theme == SeasonalTheme.HALLOWEEN && animationsEnabled) {
        // Кадры нужны только пока паук на нити (~4 с из 20): остальное время спим, а не
        // перерисовываем кнопку и шторки покадрово с невидимым пауком.
        LaunchedEffect(Unit) {
            while (true) {
                val start = withFrameNanos { it }
                do {
                    val now = withFrameNanos { it }
                    phase.floatValue = (now - start) / NanosPerMilli / SpiderCycleMillis
                } while (phase.floatValue < ClimbEnd)
                phase.floatValue = ClimbEnd
                delay(((1f - ClimbEnd) * SpiderCycleMillis).toLong())
            }
        }
    }
    return phase
}

/** Доля спуска паука (0..1) в точке цикла [phase]: спуск, пауза, подъём, долгое ожидание. */
private fun spiderDrop(phase: Float): Float = when {
    phase < DropEnd -> phase / DropEnd
    phase < HoldEnd -> 1f
    phase < ClimbEnd -> 1f - (phase - HoldEnd) / (ClimbEnd - HoldEnd)
    else -> 0f
}

/** Паук на нити из точки ([x], 0): спускается не глубже [maxLength] по фазе [phase]. */
internal fun DrawScope.drawSpiderOnThread(x: Float, maxLength: Float, phase: Float) {
    val drop = spiderDrop(phase)
    if (drop <= 0f) return
    val length = maxLength * drop
    drawLine(ThreadColor, Offset(x, 0f), Offset(x, length), strokeWidth = 0.8.dp.toPx())
    drawSpider(Offset(x, length + 4.dp.toPx()), 4.dp.toPx())
}
