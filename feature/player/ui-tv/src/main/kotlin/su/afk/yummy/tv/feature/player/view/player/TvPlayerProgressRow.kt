package su.afk.yummy.tv.feature.player.view.player

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.feature.player.utils.formatTime

@Composable
internal fun TvPlayerProgressRow(
    wantsPlay: Boolean,
    displayTime: Long,
    duration: Long,
    isSeeking: Boolean,
    seekProgress: Float,
    bufferedProgress: Float,
    currentPosition: Long,
    playFocusRequester: FocusRequester,
    playUpFocusRequester: FocusRequester?,
    progressFocusRequester: FocusRequester,
    progressDownFocusRequester: FocusRequester,
    onPlayPause: () -> Unit,
    onSeekChange: (Float) -> Unit,
    onSeekFinished: () -> Unit,
    onInteraction: () -> Unit,
) {
    var sliderJustFocused by remember { mutableStateOf(false) }
    var sliderFocused by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        TvControlButton(
            onClick = { onPlayPause(); onInteraction() },
            onFocused = onInteraction,
            focusRequester = playFocusRequester,
            modifier = Modifier.focusProperties {
                if (playUpFocusRequester != null) up = playUpFocusRequester
                down = progressFocusRequester
            },
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
        PlayerBufferedSlider(
            value = when {
                isSeeking -> seekProgress
                duration > 0 -> currentPosition.toFloat() / duration
                else -> 0f
            },
            bufferedProgress = bufferedProgress,
            onValueChange = { v ->
                if (sliderJustFocused) {
                    sliderJustFocused = false
                } else {
                    onSeekChange(v)
                    onInteraction()
                }
            },
            onValueChangeFinished = onSeekFinished,
            focused = sliderFocused,
            modifier = Modifier
                .weight(1f)
                .focusRequester(progressFocusRequester)
                .focusProperties {
                    up = playFocusRequester
                    down = progressDownFocusRequester
                }
                .onPreviewKeyEvent { event ->
                    when (event.key) {
                        Key.DirectionUp -> {
                            when (event.type) {
                                KeyEventType.KeyDown -> playFocusRequester.requestFocus()
                                KeyEventType.KeyUp -> Unit
                                else -> return@onPreviewKeyEvent false
                            }
                            true
                        }

                        Key.DirectionDown -> {
                            when (event.type) {
                                KeyEventType.KeyDown -> progressDownFocusRequester.requestFocus()
                                KeyEventType.KeyUp -> Unit
                                else -> return@onPreviewKeyEvent false
                            }
                            true
                        }

                        else -> false
                    }
                }
                .onFocusChanged {
                    sliderFocused = it.isFocused
                    if (it.isFocused) sliderJustFocused = true
                },
        )
        Text(
            text = formatTime(duration),
            style = MaterialTheme.typography.labelMedium,
            color = Color.White,
        )
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun PlayerBufferedSlider(
    value: Float,
    bufferedProgress: Float,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: () -> Unit,
    focused: Boolean,
    modifier: Modifier = Modifier,
) {
    val activeProgress = value.coerceIn(0f, 1f)
    val buffered = bufferedProgress.coerceIn(activeProgress, 1f)
    val focusedColor = MaterialTheme.colorScheme.primary
    val sliderColor by animateColorAsState(
        targetValue = if (focused) focusedColor else Color.White,
        animationSpec = tween(TV_PLAYER_FOCUS_ANIMATION_DURATION_MS),
        label = "tvPlayerProgressColor",
    )
    val bufferedColor by animateColorAsState(
        targetValue = if (focused) focusedColor.copy(alpha = 0.58f) else Color.White.copy(alpha = 0.42f),
        animationSpec = tween(TV_PLAYER_FOCUS_ANIMATION_DURATION_MS),
        label = "tvPlayerBufferedProgressColor",
    )
    val inactiveTrackColor by animateColorAsState(
        targetValue = if (focused) focusedColor.copy(alpha = 0.30f) else Color.White.copy(alpha = 0.30f),
        animationSpec = tween(TV_PLAYER_FOCUS_ANIMATION_DURATION_MS),
        label = "tvPlayerInactiveProgressColor",
    )
    val colors = SliderDefaults.colors(
        thumbColor = sliderColor,
        activeTrackColor = sliderColor,
        inactiveTrackColor = inactiveTrackColor,
        activeTickColor = Color.Transparent,
        inactiveTickColor = Color.Transparent,
    )
    Slider(
        value = activeProgress,
        onValueChange = onValueChange,
        onValueChangeFinished = onValueChangeFinished,
        modifier = modifier,
        colors = colors,
        track = { sliderState ->
            SliderDefaults.Track(
                sliderState = sliderState,
                enabled = true,
                colors = colors,
                modifier = Modifier.bufferedTrackOverlay(
                    activeProgress = activeProgress,
                    bufferedProgress = buffered,
                    color = bufferedColor,
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
