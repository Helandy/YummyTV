package su.afk.yummy.tv.core.designsystem.motion

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import su.afk.yummy.tv.core.designsystem.skeleton.rememberAnimationsEnabled

/**
 * Прозрачность для плавного проявления нового состояния экрана (скелетон → контент, ошибка и т.п.).
 *
 * Вызывается над `when` по состоянию, результат вешается через [stateChangeFade] на ветку.
 * Прежнее состояние не удерживается в композиции — в отличие от `AnimatedContent`, уходящая ветка
 * не перерисовывается с уже изменившимися данными и не оставляет на ТВ фокус на исчезающем узле.
 *
 * Первое появление не анимируется: при возврате на экран с готовыми данными мигания нет.
 */
@Composable
fun rememberStateChangeFade(stateKey: Any?): State<Float> {
    val animationsEnabled = rememberAnimationsEnabled()
    val isInitial = remember { booleanArrayOf(true) }
    val alpha = remember(stateKey) {
        Animatable(if (isInitial[0] || !animationsEnabled) 1f else 0f)
    }
    LaunchedEffect(alpha) {
        isInitial[0] = false
        alpha.animateTo(1f, tween(YummyMotion.Short))
    }
    return alpha.asState()
}

/** Применяет прозрачность из [rememberStateChangeFade] без рекомпозиции на каждом кадре. */
fun Modifier.stateChangeFade(alpha: State<Float>): Modifier = graphicsLayer { this.alpha = alpha.value }
