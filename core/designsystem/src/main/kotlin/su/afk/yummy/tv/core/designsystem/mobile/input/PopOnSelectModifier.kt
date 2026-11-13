package su.afk.yummy.tv.core.designsystem.mobile.input

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import su.afk.yummy.tv.core.designsystem.motion.YummyMotion

private const val POP_SCALE = 1.25f

/**
 * «Подпрыгивание» иконки, когда [selected] становится `true` (лайк, избранное, подписка).
 * Первое появление не анимируется, чтобы уже отмеченные элементы не прыгали при прокрутке списка.
 */
@Composable
fun Modifier.popOnSelect(selected: Boolean): Modifier {
    val scale = remember { Animatable(1f) }
    val isInitial = remember { booleanArrayOf(true) }
    LaunchedEffect(selected) {
        if (isInitial[0]) {
            isInitial[0] = false
            return@LaunchedEffect
        }
        if (selected) scale.animateTo(POP_SCALE, tween(YummyMotion.Quick))
        scale.animateTo(1f, YummyMotion.scaleSpring())
    }
    return graphicsLayer {
        scaleX = scale.value
        scaleY = scale.value
    }
}
