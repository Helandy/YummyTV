package su.afk.yummy.tv.feature.player.view.player

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.feature.player.utils.formatTime

@Composable
internal fun PlayerProgressRow(
    wantsPlay: Boolean,
    displayTime: Long,
    duration: Long,
    isSeeking: Boolean,
    seekProgress: Float,
    currentPosition: Long,
    playFocusRequester: FocusRequester,
    onPlayPause: () -> Unit,
    onSeekChange: (Float) -> Unit,
    onSeekFinished: () -> Unit,
    onInteraction: () -> Unit,
) {
    val focusManager = LocalFocusManager.current
    var sliderJustFocused by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ControlButton(
            onClick = { onPlayPause(); onInteraction() },
            onFocused = onInteraction,
            focusRequester = playFocusRequester,
            primary = true,
        ) { color ->
            Icon(
                imageVector = if (wantsPlay) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                contentDescription = null,
                tint = color,
                modifier = Modifier.then(Modifier).let {
                    it
                },
            )
        }
        Text(
            text = formatTime(displayTime),
            style = MaterialTheme.typography.labelMedium,
            color = Color.White,
        )
        Slider(
            value = when {
                isSeeking -> seekProgress
                duration > 0 -> currentPosition.toFloat() / duration
                else -> 0f
            },
            onValueChange = { v ->
                if (sliderJustFocused) {
                    sliderJustFocused = false
                } else {
                    onSeekChange(v)
                    onInteraction()
                }
            },
            onValueChangeFinished = onSeekFinished,
            modifier = Modifier
                .weight(1f)
                .onFocusChanged { if (it.isFocused) sliderJustFocused = true }
                .onPreviewKeyEvent { event ->
                    if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                    when (event.key) {
                        Key.DirectionUp -> {
                            focusManager.moveFocus(FocusDirection.Up); true
                        }

                        Key.DirectionDown -> {
                            focusManager.moveFocus(FocusDirection.Down); true
                        }

                        else -> false
                    }
                },
            colors = SliderDefaults.colors(
                thumbColor = Color.White,
                activeTrackColor = Color.White,
                inactiveTrackColor = Color.White.copy(alpha = 0.30f),
                activeTickColor = Color.Transparent,
                inactiveTickColor = Color.Transparent,
            ),
        )
        Text(
            text = formatTime(duration),
            style = MaterialTheme.typography.labelMedium,
            color = Color.White,
        )
    }
}
