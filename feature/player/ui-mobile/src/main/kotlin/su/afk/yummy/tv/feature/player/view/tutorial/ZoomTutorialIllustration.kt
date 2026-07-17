package su.afk.yummy.tv.feature.player.view.tutorial

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke

@Composable
internal fun ZoomTutorialIllustration(accent: Color) {
    val progress = rememberTutorialLoopProgress("zoom", reverse = true)
    Canvas(
        Modifier
            .fillMaxWidth(0.9f)
            .aspectRatio(16f / 9f)
    ) {
        drawTutorialScreenFrame()
        val scale = 0.62f + progress * 0.24f
        val frameSize = Size(size.width * scale, size.height * scale)
        val frameTopLeft = Offset(
            x = (size.width - frameSize.width) / 2f,
            y = (size.height - frameSize.height) / 2f,
        )
        drawRoundRect(
            color = accent.copy(alpha = 0.72f),
            topLeft = frameTopLeft,
            size = frameSize,
            cornerRadius = CornerRadius(18f),
            style = Stroke(width = 5f),
        )
        val fingerDistance = size.width * (0.07f + progress * 0.15f)
        val first = center - Offset(fingerDistance, fingerDistance * 0.38f)
        val second = center + Offset(fingerDistance, fingerDistance * 0.38f)
        drawCircle(Color.White.copy(alpha = 0.32f), 24f, first)
        drawCircle(Color.White, 11f, first)
        drawCircle(Color.White.copy(alpha = 0.32f), 24f, second)
        drawCircle(Color.White, 11f, second)
        drawLine(accent, center, first, strokeWidth = 4f, cap = StrokeCap.Round)
        drawLine(accent, center, second, strokeWidth = 4f, cap = StrokeCap.Round)
    }
}
