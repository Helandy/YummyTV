package su.afk.yummy.tv.feature.player.mobile.view.tutorial

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import kotlin.math.abs

@Composable
internal fun TapTutorialIllustration(accent: Color) {
    val progress = rememberTutorialLoopProgress("tap")
    Canvas(
        Modifier
            .fillMaxWidth(0.9f)
            .aspectRatio(16f / 9f)
    ) {
        drawTutorialScreenFrame()
        val alpha = 1f - progress
        drawCircle(
            color = accent.copy(alpha = alpha * 0.35f),
            radius = 34f + 50f * progress,
            center = center,
        )
        drawCircle(Color.White.copy(alpha = 0.94f), radius = 13f, center = center)
        val controlsAlpha = 0.18f + 0.55f * (1f - abs(progress - 0.5f) * 2f)
        drawRoundRect(
            color = Color.White.copy(alpha = controlsAlpha),
            topLeft = Offset(size.width * 0.12f, size.height * 0.10f),
            size = Size(size.width * 0.76f, 7f),
            cornerRadius = CornerRadius(6f),
        )
        drawRoundRect(
            color = Color.White.copy(alpha = controlsAlpha),
            topLeft = Offset(size.width * 0.12f, size.height * 0.82f),
            size = Size(size.width * 0.76f, 12f),
            cornerRadius = CornerRadius(8f),
        )
    }
}
