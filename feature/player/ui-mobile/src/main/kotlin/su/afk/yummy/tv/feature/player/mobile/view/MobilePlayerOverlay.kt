package su.afk.yummy.tv.feature.player.mobile.view

import androidx.compose.foundation.background
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.remember
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.feature.player.common.model.PlayerPlaybackProgressState
import su.afk.yummy.tv.feature.player.presentation.R

@Composable
internal fun MobilePlayerOverlay(
    visible: Boolean,
    wantsPlay: Boolean,
    progress: PlayerPlaybackProgressState,
    openingStartMs: Long?,
    openingEndMs: Long?,
    hasPrevEpisode: Boolean,
    onPlayPause: () -> Unit,
    onSeekChange: (Float) -> Unit,
    onSeekFinished: () -> Unit,
    onPrevEpisode: () -> Unit,
    onNextEpisode: () -> Unit,
    onTrackSettings: () -> Unit,
    onPlayerSettings: () -> Unit,
    modifier: Modifier = Modifier,
    /** Ручка и раскрывающаяся лента серий под кнопками управления (ландшафт). */
    episodesAvailable: Boolean = false,
    episodesExpanded: Boolean = false,
    /** Лента серий поверх видео (ландшафт); в портрете раскрытие показывает список под плеером. */
    episodesStripVisible: Boolean = false,
    onEpisodesExpandedChange: (Boolean) -> Unit = {},
    episodesStrip: @Composable () -> Unit = {},
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
            displayTime = progress.displayTimeMs,
            duration = progress.duration,
            seekProgress = progress.progressFraction,
            bufferedProgress = progress.bufferedProgress,
            openingStartMs = openingStartMs,
            openingEndMs = openingEndMs,
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
                    contentDescription = stringResource(R.string.player_mobile_previous_episode),
                    enabled = hasPrevEpisode,
                    onClick = onPrevEpisode,
                )
                MobilePlayerActionButton(
                    icon = Icons.AutoMirrored.Filled.VolumeUp,
                    contentDescription = stringResource(R.string.player_mobile_track_settings_title),
                    onClick = onTrackSettings,
                )
            }
            Spacer(Modifier.weight(1f))
            MobilePlayerActionButton(
                icon = if (wantsPlay) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                contentDescription = stringResource(
                    if (wantsPlay) R.string.player_mobile_pip_pause else R.string.player_mobile_pip_play,
                ),
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
                    contentDescription = stringResource(R.string.player_mobile_playback_settings_title),
                    onClick = onPlayerSettings,
                )
                MobilePlayerActionButton(
                    icon = Icons.Filled.SkipNext,
                    contentDescription = stringResource(R.string.player_mobile_next_episode),
                    onClick = onNextEpisode,
                )
            }
        }
        if (episodesAvailable) {
            EpisodesHandle(
                expanded = episodesExpanded,
                onExpandedChange = onEpisodesExpandedChange,
            )
            AnimatedVisibility(
                visible = episodesStripVisible,
                enter = expandVertically(),
                exit = shrinkVertically(),
                modifier = Modifier.pointerInput(Unit) { collapseOnSwipeDown(onEpisodesExpandedChange) },
            ) {
                episodesStrip()
            }
        }
    }
}

/** Ручка под кнопками: тап или свайп вверх раскрывает ленту серий, свайп вниз — сворачивает. */
@Composable
private fun EpisodesHandle(
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .pointerInput(expanded) {
                var total = 0f
                detectVerticalDragGestures(
                    onDragStart = { total = 0f },
                    onVerticalDrag = { change, delta ->
                        change.consume()
                        total += delta
                    },
                    onDragEnd = {
                        if (total < -SWIPE_THRESHOLD_PX) onExpandedChange(true)
                        if (total > SWIPE_THRESHOLD_PX) onExpandedChange(false)
                    },
                )
            }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ) { onExpandedChange(!expanded) },
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = if (expanded) Icons.Filled.KeyboardArrowDown else Icons.Filled.KeyboardArrowUp,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(18.dp),
        )
        Text(
            text = stringResource(R.string.player_episodes_list_title),
            style = MaterialTheme.typography.labelMedium,
            color = Color.White,
        )
    }
}

private suspend fun PointerInputScope.collapseOnSwipeDown(onExpandedChange: (Boolean) -> Unit) {
    var total = 0f
    detectVerticalDragGestures(
        onDragStart = { total = 0f },
        onVerticalDrag = { _, delta -> total += delta },
        onDragEnd = { if (total > SWIPE_THRESHOLD_PX) onExpandedChange(false) },
    )
}

private const val SWIPE_THRESHOLD_PX = 40f
