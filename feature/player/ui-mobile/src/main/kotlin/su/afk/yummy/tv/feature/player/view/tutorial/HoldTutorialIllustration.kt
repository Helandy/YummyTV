package su.afk.yummy.tv.feature.player.view.tutorial

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
internal fun HoldTutorialIllustration(accent: Color) {
    val progress = rememberTutorialLoopProgress("hold")
    Box(
        modifier = Modifier
            .fillMaxWidth(0.9f)
            .aspectRatio(16f / 9f),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            drawTutorialScreenFrame()
            drawCircle(Color.White.copy(alpha = 0.14f), 58f, center)
            drawCircle(Color.White.copy(alpha = 0.95f), 15f, center)
            drawArc(
                color = accent,
                startAngle = -90f,
                sweepAngle = progress * 360f,
                useCenter = false,
                topLeft = center - Offset(48f, 48f),
                size = Size(96f, 96f),
                style = Stroke(width = 7f, cap = StrokeCap.Round),
            )
        }
        Text(
            text = "2×",
            color = Color.White,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .align(Alignment.Center)
                .padding(top = 112.dp),
        )
    }
}
