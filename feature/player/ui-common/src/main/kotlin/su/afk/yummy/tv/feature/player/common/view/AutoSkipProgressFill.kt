package su.afk.yummy.tv.feature.player.common.view

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.unit.Dp
import su.afk.yummy.tv.feature.player.common.PlayerSkipUiState

/**
 * Заливка кнопки пропуска слева направо по мере отсчёта автопропуска.
 * [progress] — доля 0..1 или null, если отсчёта нет. Ставить после background, до padding.
 */
@Composable
fun Modifier.autoSkipProgressFill(progress: Float?, color: Color, cornerRadius: Dp): Modifier {
    // Отсчёт тикает шагами; линейная анимация на тот же шаг делает заливку плавной.
    val animated by animateFloatAsState(
        targetValue = progress ?: 0f,
        animationSpec = if (progress == null || progress == 0f) {
            tween(0)
        } else {
            tween(PlayerSkipUiState.AUTO_SKIP_TICK_MS.toInt(), easing = LinearEasing)
        },
        label = "autoSkipProgressFill",
    )
    if (progress == null) return this
    return drawBehind {
        clipRect(right = size.width * animated) {
            drawRoundRect(color = color, cornerRadius = CornerRadius(cornerRadius.toPx()))
        }
    }
}
