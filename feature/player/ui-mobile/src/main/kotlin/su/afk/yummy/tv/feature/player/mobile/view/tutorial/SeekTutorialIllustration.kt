package su.afk.yummy.tv.feature.player.mobile.view.tutorial

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color

@Composable
internal fun SeekTutorialIllustration(accent: Color) {
    val progress = rememberTutorialLoopProgress("seek")
    Canvas(
        Modifier
            .fillMaxWidth(0.9f)
            .aspectRatio(16f / 9f)
    ) {
        drawTutorialScreenFrame()
        drawRect(
            color = Color.White.copy(alpha = 0.055f),
            size = Size(size.width / 2f, size.height),
        )
        drawRect(
            color = Color.White.copy(alpha = 0.09f),
            topLeft = Offset(size.width / 2f, 0f),
            size = Size(size.width / 2f, size.height),
        )
        val leftCenter = Offset(size.width * 0.27f, size.height * 0.5f)
        val rightCenter = Offset(size.width * 0.73f, size.height * 0.5f)
        val activeCenter = if (progress < 0.5f) leftCenter else rightCenter
        val localProgress = (progress * 2f) % 1f
        drawCircle(
            color = accent.copy(alpha = (1f - localProgress) * 0.4f),
            radius = 30f + 46f * localProgress,
            center = activeCenter,
        )
        drawCircle(Color.White.copy(alpha = 0.92f), 11f, activeCenter)
        drawSeekChevrons(
            center = leftCenter,
            pointsRight = false,
            color = Color.White.copy(alpha = 0.78f),
        )
        drawSeekChevrons(
            center = rightCenter,
            pointsRight = true,
            color = Color.White.copy(alpha = 0.78f),
        )
    }
}
