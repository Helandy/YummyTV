package su.afk.yummy.tv.feature.player.mobile.view

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Tune
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
internal fun MobilePlayerOverlay(
    visible: Boolean,
    wantsPlay: Boolean,
    displayTime: Long,
    duration: Long,
    seekProgress: Float,
    bufferedProgress: Float,
    hasPrevEpisode: Boolean,
    hasNextEpisode: Boolean,
    onPlayPause: () -> Unit,
    onSeekChange: (Float) -> Unit,
    onSeekFinished: () -> Unit,
    onPrevEpisode: () -> Unit,
    onNextEpisode: () -> Unit,
    onTrackSettings: () -> Unit,
    onPlaybackSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (!visible) return

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color.Transparent,
                        Color.Black.copy(alpha = 0.90f)
                    )
                )
            )
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = 18.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        MobilePlayerProgressRow(
            displayTime = displayTime,
            duration = duration,
            seekProgress = seekProgress,
            bufferedProgress = bufferedProgress,
            onSeekChange = onSeekChange,
            onSeekFinished = onSeekFinished,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                MobilePlayerActionButton(
                    icon = Icons.Filled.SkipPrevious,
                    enabled = hasPrevEpisode,
                    onClick = onPrevEpisode,
                )
                MobilePlayerActionButton(
                    icon = Icons.AutoMirrored.Filled.VolumeUp,
                    onClick = onTrackSettings,
                )
            }
            Spacer(Modifier.weight(1f))
            MobilePlayerActionButton(
                icon = if (wantsPlay) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                primary = true,
                onClick = onPlayPause,
            )
            Spacer(Modifier.weight(1f))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                MobilePlayerActionButton(
                    icon = Icons.Filled.Tune,
                    onClick = onPlaybackSettings,
                )
                MobilePlayerActionButton(
                    icon = Icons.Filled.SkipNext,
                    enabled = hasNextEpisode,
                    onClick = onNextEpisode,
                )
            }
        }
    }
}
