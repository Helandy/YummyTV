package su.afk.yummy.tv.feature.player.mobile.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.feature.player.mobile.utils.formatMobilePlayerTime

@Composable
internal fun MobilePlayerProgressRow(
    displayTime: Long,
    duration: Long,
    seekProgress: Float,
    bufferedProgress: Float,
    onSeekChange: (Float) -> Unit,
    onSeekFinished: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = formatMobilePlayerTime(displayTime),
            style = MaterialTheme.typography.labelMedium,
            color = Color.White,
        )
        MobileBufferedSlider(
            value = seekProgress.coerceIn(0f, 1f),
            bufferedProgress = bufferedProgress,
            onValueChange = onSeekChange,
            onValueChangeFinished = onSeekFinished,
            enabled = duration > 0,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = formatMobilePlayerTime(duration),
            style = MaterialTheme.typography.labelMedium,
            color = Color.White.copy(alpha = 0.82f),
        )
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun MobileBufferedSlider(
    value: Float,
    bufferedProgress: Float,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    val activeProgress = value.coerceIn(0f, 1f)
    val buffered = bufferedProgress.coerceIn(activeProgress, 1f)
    val colors = SliderDefaults.colors(
        thumbColor = Color.White,
        activeTrackColor = Color.White,
        inactiveTrackColor = Color.White.copy(alpha = 0.28f),
        disabledThumbColor = Color.White.copy(alpha = 0.42f),
        disabledActiveTrackColor = Color.White.copy(alpha = 0.32f),
        disabledInactiveTrackColor = Color.White.copy(alpha = 0.16f),
        activeTickColor = Color.Transparent,
        inactiveTickColor = Color.Transparent,
    )
    Slider(
        value = activeProgress,
        onValueChange = onValueChange,
        onValueChangeFinished = onValueChangeFinished,
        enabled = enabled,
        modifier = modifier,
        colors = colors,
        track = { sliderState ->
            SliderDefaults.Track(
                sliderState = sliderState,
                enabled = enabled,
                colors = colors,
                modifier = Modifier.bufferedTrackOverlay(
                    activeProgress = activeProgress,
                    bufferedProgress = buffered,
                    color = Color.White.copy(alpha = 0.45f),
                ),
            )
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
private fun Modifier.bufferedTrackOverlay(
    activeProgress: Float,
    bufferedProgress: Float,
    color: Color,
): Modifier =
    drawWithContent {
        drawContent()
        if (bufferedProgress <= activeProgress) return@drawWithContent

        val gapPx = 6.dp.toPx()
        val strokeWidth = 4.dp.toPx()
        val trackY = size.height / 2f
        val startX: Float
        val endX: Float
        if (layoutDirection == LayoutDirection.Rtl) {
            startX = (size.width * (1f - activeProgress) - gapPx).coerceIn(0f, size.width)
            endX = (size.width * (1f - bufferedProgress)).coerceIn(0f, size.width)
        } else {
            startX = (size.width * activeProgress + gapPx).coerceIn(0f, size.width)
            endX = (size.width * bufferedProgress).coerceIn(0f, size.width)
        }
        if (startX == endX) return@drawWithContent
        drawLine(
            color = color,
            start = Offset(startX, trackY),
            end = Offset(endX, trackY),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round,
        )
    }
