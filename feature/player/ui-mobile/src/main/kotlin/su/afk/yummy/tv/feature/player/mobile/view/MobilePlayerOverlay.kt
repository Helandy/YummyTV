package su.afk.yummy.tv.feature.player.mobile.view

import androidx.compose.foundation.background
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VideoLibrary
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
    onMoreActions: () -> Unit,
    modifier: Modifier = Modifier,
    /** Кнопка серий в ряду управления и раскрывающаяся горизонтальная лента под ним. */
    episodesAvailable: Boolean = false,
    episodesExpanded: Boolean = false,
    /** Горизонтальная лента серий поверх видео (и в портрете, и в ландшафте). */
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
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.Start),
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
                MobilePlayerActionButton(
                    icon = Icons.Filled.MoreVert,
                    contentDescription = stringResource(R.string.player_mobile_actions),
                    onClick = onMoreActions,
                )
            }
            MobilePlayerActionButton(
                icon = if (wantsPlay) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                contentDescription = stringResource(
                    if (wantsPlay) R.string.player_mobile_pip_pause else R.string.player_mobile_pip_play,
                ),
                primary = true,
                onClick = onPlayPause,
            )
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (episodesAvailable) {
                    MobilePlayerActionButton(
                        icon = Icons.Filled.VideoLibrary,
                        contentDescription = stringResource(R.string.player_episodes_list_title),
                        onClick = { onEpisodesExpandedChange(!episodesExpanded) },
                    )
                }
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
            AnimatedVisibility(
                visible = episodesStripVisible,
                enter = expandVertically(animationSpec = tween(EPISODES_STRIP_ANIMATION_MS)),
                exit = shrinkVertically(animationSpec = tween(EPISODES_STRIP_ANIMATION_MS)),
                modifier = Modifier.pointerInput(Unit) { collapseOnSwipeDown(onEpisodesExpandedChange) },
            ) {
                episodesStrip()
            }
        }
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

/** Линейная анимация вместо дефолтной пружины: у пружины длинный хвост, и лента «доезжает» с лагом. */
private const val EPISODES_STRIP_ANIMATION_MS = 220
