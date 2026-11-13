package su.afk.yummy.tv.core.designsystem.mobile.input

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import su.afk.yummy.tv.core.designsystem.motion.YummyMotion

private const val PRESSED_SCALE = 0.97f

/**
 * Лёгкое «проседание» карточки под пальцем. [interactionSource] — тот же, что отдан в
 * `clickable`/`combinedClickable`. Ставить в начало цепочки модификаторов, до фона и обрезки,
 * иначе масштабируется только содержимое.
 */
@Composable
fun Modifier.pressScale(
    interactionSource: InteractionSource,
    pressedScale: Float = PRESSED_SCALE,
): Modifier {
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) pressedScale else 1f,
        animationSpec = YummyMotion.scaleSpring(),
        label = "pressScale",
    )
    return graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}
