package su.afk.yummy.tv.feature.player.view.tutorial

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import su.afk.yummy.tv.feature.player.model.TvControlsTutorialStep

/** Схема пульта с подсвеченной активной зоной для текущего шага обучения. */
@Composable
internal fun TvControlsTutorialIllustration(
    step: TvControlsTutorialStep,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    val transition = rememberInfiniteTransition(label = "tvTutorialPulse")
    val pulse by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "tvTutorialPulseValue",
    )

    Canvas(modifier = modifier.aspectRatio(0.62f)) {
        val w = size.width
        val h = size.height
        val bodyColor = Color.White.copy(alpha = 0.10f)
        val strokeColor = Color.White.copy(alpha = 0.35f)
        val dimColor = Color.White.copy(alpha = 0.28f)

        // Корпус пульта.
        val bodyStroke = Stroke(width = w * 0.02f)
        drawRoundRect(
            color = bodyColor,
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.18f, w * 0.18f),
        )
        drawRoundRect(
            color = strokeColor,
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.18f, w * 0.18f),
            style = bodyStroke,
        )

        val center = Offset(w / 2f, h * 0.34f)
        val dpadRadius = w * 0.30f

        fun highlight(target: Offset, radius: Float, active: Boolean) {
            val color = if (active) accent else dimColor
            if (active) {
                drawCircle(
                    color = accent.copy(alpha = (1f - pulse) * 0.5f),
                    radius = radius * (1f + pulse * 0.6f),
                    center = target,
                )
            }
            drawCircle(color = color, radius = radius, center = target)
        }

        // Кольцо D-pad.
        drawCircle(
            color = strokeColor,
            radius = dpadRadius,
            center = center,
            style = Stroke(width = w * 0.015f),
        )

        val keyRadius = w * 0.075f
        val up = center - Offset(0f, dpadRadius * 0.62f)
        val down = center + Offset(0f, dpadRadius * 0.62f)
        val left = center - Offset(dpadRadius * 0.62f, 0f)
        val right = center + Offset(dpadRadius * 0.62f, 0f)

        highlight(up, keyRadius, step == TvControlsTutorialStep.Navigate)
        highlight(down, keyRadius, step == TvControlsTutorialStep.Navigate)
        highlight(left, keyRadius, step == TvControlsTutorialStep.Seek)
        highlight(right, keyRadius, step == TvControlsTutorialStep.Seek)
        highlight(
            center,
            keyRadius * 1.15f,
            step == TvControlsTutorialStep.ShowControls || step == TvControlsTutorialStep.PlayPause,
        )

        // Нижний ряд — кнопки панелей (озвучка/качество/скорость/громкость).
        val rowY = h * 0.78f
        val rowActive = step == TvControlsTutorialStep.Panels
        val slots = 4
        val spacing = w * 0.16f
        val startX = w / 2f - spacing * (slots - 1) / 2f
        repeat(slots) { i ->
            val x = startX + spacing * i
            highlight(Offset(x, rowY), w * 0.045f, rowActive)
        }

        // Индикатор "Назад" сверху.
        highlight(
            Offset(w * 0.22f, h * 0.10f),
            w * 0.045f,
            step == TvControlsTutorialStep.Exit,
        )
    }
}
