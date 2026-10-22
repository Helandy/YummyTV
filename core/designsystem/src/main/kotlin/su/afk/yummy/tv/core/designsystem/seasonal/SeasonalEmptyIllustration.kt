package su.afk.yummy.tv.core.designsystem.seasonal

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import su.afk.yummy.tv.core.designsystem.skeleton.rememberAnimationsEnabled

private const val FloatMillis = 2_400

/** Хэллоуинская картинка пустого состояния: покачивающийся призрак рядом с надгробием. */
@Composable
fun SeasonalEmptyIllustration(modifier: Modifier = Modifier) {
    val bob: State<Float> = if (rememberAnimationsEnabled()) {
        rememberInfiniteTransition(label = "emptyGhost").animateFloat(
            initialValue = -1f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(FloatMillis, easing = FastOutSlowInEasing), RepeatMode.Reverse),
            label = "emptyGhostBob",
        )
    } else {
        remember { mutableFloatStateOf(0f) }
    }
    Canvas(modifier) {
        val h = size.height
        val w = size.width
        drawTombstone(Offset(w * 0.78f, h), h * 0.42f)
        drawGhost(Offset(w * 0.4f, h * 0.48f + bob.value * h * 0.04f), h * 0.74f, 0.95f)
    }
}
