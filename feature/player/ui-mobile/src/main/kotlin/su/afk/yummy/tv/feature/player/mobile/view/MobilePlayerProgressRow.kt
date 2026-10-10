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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.core.utils.formatting.millisToClockTime
import su.afk.yummy.tv.feature.player.common.utils.openingRange
import su.afk.yummy.tv.feature.player.common.view.bufferedTrackOverlay

@Composable
internal fun MobilePlayerProgressRow(
    displayTime: Long,
    duration: Long,
    seekProgress: Float,
    bufferedProgress: Float,
    openingStartMs: Long?,
    openingEndMs: Long?,
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
            text = displayTime.millisToClockTime(),
            style = MaterialTheme.typography.labelMedium,
            color = Color.White,
        )
        MobileBufferedSlider(
            value = seekProgress.coerceIn(0f, 1f),
            bufferedProgress = bufferedProgress,
            openingRange = openingRange(openingStartMs, openingEndMs, duration),
            onValueChange = onSeekChange,
            onValueChangeFinished = onSeekFinished,
            enabled = duration > 0,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = duration.millisToClockTime(),
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
    openingRange: ClosedFloatingPointRange<Float>?,
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
                    openingRange = openingRange,
                    openingColor = MaterialTheme.colorScheme.primary,
                ),
            )
        },
    )
}
