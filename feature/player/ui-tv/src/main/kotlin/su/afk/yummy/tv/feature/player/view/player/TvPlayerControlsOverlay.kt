package su.afk.yummy.tv.feature.player.view.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.feature.player.model.ActiveSkip
import su.afk.yummy.tv.feature.player.model.PlayerPlaybackUiState
import su.afk.yummy.tv.feature.player.model.TvPlaybackProgressState
import su.afk.yummy.tv.feature.player.model.TvPlayerFocusRequesters
import su.afk.yummy.tv.feature.player.presentation.R

/** Нижний блок контролов: кнопка пропуска, прогресс и ряд эпизода/настроек. */
@Composable
internal fun BoxScope.TvPlayerControlsOverlay(
    visible: Boolean,
    focus: TvPlayerFocusRequesters,
    progress: TvPlaybackProgressState,
    bufferedProgress: Float,
    wantsPlay: Boolean,
    playback: PlayerPlaybackUiState,
    animeTitle: String,
    activeSkip: ActiveSkip?,
    autoSkipOpeningsEndings: Boolean,
    highlightedSkipKey: String?,
    qualityCount: Int,
    currentQualityLabel: String,
    currentSpeedLabel: String,
    onPlayPause: () -> Unit,
    onSeekTo: (positionMs: Long) -> Unit,
    onInteraction: () -> Unit,
    onSkipActiveSegment: () -> Unit,
    onPrevEpisode: () -> Unit,
    onNextEpisode: () -> Unit,
    onRateTitle: () -> Unit,
    onToggleQuality: () -> Unit,
    onToggleDubbing: () -> Unit,
    onToggleBalancer: () -> Unit,
    onToggleResize: () -> Unit,
    onToggleSpeed: () -> Unit,
) {
    val visibleSkip = activeSkip.takeUnless { autoSkipOpeningsEndings }
    val progressDownFocusRequester = when {
        playback.dubbingNames.size > 1 -> focus.dubbing
        playback.balancerNames.size > 1 -> focus.balancer
        qualityCount > 0 -> focus.quality
        else -> focus.resize
    }

    AnimatedVisibility(
        visible = visible,
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .fillMaxWidth(),
        enter = fadeIn(),
        exit = fadeOut(),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.88f)
                        )
                    )
                ),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 48.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                if (visibleSkip != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        TvControlButton(
                            onClick = onSkipActiveSegment,
                            onFocused = onInteraction,
                            focusRequester = focus.skip,
                            modifier = Modifier.focusProperties { down = focus.play },
                            primary = highlightedSkipKey == visibleSkip.key,
                        ) { color ->
                            Text(
                                text = stringResource(R.string.player_skip_segment),
                                style = MaterialTheme.typography.titleMedium,
                                color = color,
                            )
                        }
                    }
                }
                TvPlayerProgressRow(
                    wantsPlay = wantsPlay,
                    displayTime = progress.displayTimeMs,
                    duration = progress.duration,
                    isSeeking = progress.isSeeking,
                    seekProgress = progress.seekProgress,
                    bufferedProgress = bufferedProgress,
                    currentPosition = progress.currentPosition,
                    playFocusRequester = focus.play,
                    playUpFocusRequester = focus.skip.takeIf { visibleSkip != null },
                    progressFocusRequester = focus.progress,
                    progressDownFocusRequester = progressDownFocusRequester,
                    onPlayPause = onPlayPause,
                    onSeekChange = { v ->
                        progress.isSeeking = true
                        progress.seekProgress = v
                    },
                    onSeekFinished = {
                        if (progress.isSeeking) {
                            onSeekTo((progress.seekProgress * progress.duration).toLong())
                            progress.isSeeking = false
                        }
                    },
                    onInteraction = onInteraction,
                )
                TvPlayerEpisodeRow(
                    hasPrevEpisode = playback.hasPrevEpisode,
                    hasNextEpisode = playback.hasNextEpisode,
                    canRateTitle = playback.canRateTitleOnEnd && !playback.hasNextEpisode,
                    qualityCount = qualityCount,
                    allDubbingNames = playback.dubbingNames,
                    currentDubbingIndex = playback.currentDubbingIndex,
                    allBalancerNames = playback.balancerNames,
                    currentBalancerIndex = playback.currentBalancerIndex,
                    playerName = playback.activeBalancerName,
                    dubbing = playback.activeDubbing,
                    currentQualityLabel = currentQualityLabel,
                    qualityFocusRequester = focus.quality,
                    dubbingFocusRequester = focus.dubbing,
                    balancerFocusRequester = focus.balancer,
                    speedFocusRequester = focus.speed,
                    upFocusRequester = focus.progress,
                    onInteraction = onInteraction,
                    onPrevEpisode = onPrevEpisode,
                    onNextEpisode = onNextEpisode,
                    onRateTitle = onRateTitle,
                    onToggleQuality = onToggleQuality,
                    onToggleDubbing = onToggleDubbing,
                    onToggleBalancer = onToggleBalancer,
                    resizeFocusRequester = focus.resize,
                    onToggleResize = onToggleResize,
                    currentSpeedLabel = currentSpeedLabel,
                    onToggleSpeed = onToggleSpeed,
                )
            }
        }
    }
}
