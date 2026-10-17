package su.afk.yummy.tv.core.designsystem.skeleton

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloat
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color

private const val SHIMMER_DURATION_MILLIS = 1200

/**
 * Общий для всего скелетона блик: один `InfiniteTransition` на экран, а не на каждый блок.
 *
 * [progress] читается только в фазе отрисовки (см. [skeleton]), поэтому анимация перерисовывает
 * блоки, не вызывая рекомпозиций. При выключенных системных анимациях [progress] = `null` —
 * блоки рисуются статичной заливкой.
 */
@Stable
class SkeletonShimmer internal constructor(
    internal val baseColor: Color,
    internal val highlightColor: Color,
    internal val progress: State<Float>?,
)

@Composable
fun rememberSkeletonShimmer(): SkeletonShimmer {
    val onSurface = MaterialTheme.colorScheme.onSurface
    val baseColor = onSurface.copy(alpha = 0.08f)
    val highlightColor = onSurface.copy(alpha = 0.18f)
    val progress = if (rememberAnimationsEnabled()) {
        rememberInfiniteTransition(label = "skeletonShimmer").animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = SHIMMER_DURATION_MILLIS, easing = LinearEasing),
                repeatMode = RepeatMode.Restart,
            ),
            label = "skeletonShimmerProgress",
        )
    } else {
        null
    }
    return remember(baseColor, highlightColor, progress) {
        SkeletonShimmer(baseColor = baseColor, highlightColor = highlightColor, progress = progress)
    }
}
