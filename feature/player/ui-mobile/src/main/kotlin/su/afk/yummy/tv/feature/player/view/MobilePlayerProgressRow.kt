package su.afk.yummy.tv.feature.player.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.feature.player.utils.formatMobilePlayerTime

@Composable
internal fun MobilePlayerProgressRow(
    displayTime: Long,
    duration: Long,
    seekProgress: Float,
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
        Slider(
            value = seekProgress.coerceIn(0f, 1f),
            onValueChange = onSeekChange,
            onValueChangeFinished = onSeekFinished,
            enabled = duration > 0,
            modifier = Modifier.weight(1f),
            colors = SliderDefaults.colors(
                thumbColor = Color.White,
                activeTrackColor = Color.White,
                inactiveTrackColor = Color.White.copy(alpha = 0.28f),
                disabledThumbColor = Color.White.copy(alpha = 0.42f),
                disabledActiveTrackColor = Color.White.copy(alpha = 0.32f),
                disabledInactiveTrackColor = Color.White.copy(alpha = 0.16f),
                activeTickColor = Color.Transparent,
                inactiveTickColor = Color.Transparent,
            ),
        )
        Text(
            text = formatMobilePlayerTime(duration),
            style = MaterialTheme.typography.labelMedium,
            color = Color.White.copy(alpha = 0.82f),
        )
    }
}
