package su.afk.yummy.tv.core.designsystem.seasonal

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.core.designsystem.skeleton.rememberAnimationsEnabled

private val DecorationHeight = 32.dp
private const val BubbleMillis = 3_000

/**
 * Хэллоуин на верхней кромке бара: тыква в левом углу, котёл с булькающим зельем и паутина в правом. Касания не перехватывает.
 */
@Composable
fun HalloweenBarDecoration(modifier: Modifier = Modifier, height: Dp = DecorationHeight) {
    val bubbles: State<Float> = if (rememberAnimationsEnabled()) {
        rememberInfiniteTransition(label = "barCauldron").animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(BubbleMillis, easing = LinearEasing)),
            label = "barCauldronPhase",
        )
    } else {
        remember { mutableFloatStateOf(0f) }
    }
    Canvas(modifier.fillMaxWidth().height(height)) {
        val h = size.height
        val w = size.width
        // Три символа: тыква в левом углу, котёл между вкладками, паутина в правом углу. Иконки
        // вкладок на 10/30/50/70/90% ширины, поэтому котёл стоит в просвете.
        drawPumpkin(Offset(w * 0.038f, h), h * 0.55f)
        drawCauldron(Offset(w * 0.385f, h), h * 0.62f, bubbles.value)
        drawCornerWeb(Offset(w, 0f), -1f, h)
    }
}
