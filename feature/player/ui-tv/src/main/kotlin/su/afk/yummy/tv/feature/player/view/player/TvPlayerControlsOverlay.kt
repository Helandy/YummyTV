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
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.feature.player.common.model.PlayerActiveSkip
import su.afk.yummy.tv.feature.player.model.PlayerPlaybackUiState
import su.afk.yummy.tv.feature.player.model.TvPlayerFocusRequesters
import su.afk.yummy.tv.feature.player.common.model.PlayerPlaybackProgressState
import su.afk.yummy.tv.feature.player.presentation.R

/** Нижний блок контролов: кнопка пропуска, прогресс и ряд эпизода/настроек. */
@Composable
internal fun BoxScope.TvPlayerControlsOverlay(
    visible: Boolean,
    focus: TvPlayerFocusRequesters,
    progress: PlayerPlaybackProgressState,
    wantsPlay: Boolean,
    playback: PlayerPlaybackUiState,
    animeTitle: String,
    activeSkip: PlayerActiveSkip?,
    autoSkipRemainingSeconds: Int?,
    autoSkipProgress: Float?,
    showOpeningOnTimeline: Boolean,
    highlightedSkipKey: String?,
    qualityCount: Int,
    currentQualityLabel: String,
    adaptiveQuality: Boolean,
    showAllohaButton: Boolean,
    onPlayPause: () -> Unit,
    onSeekTo: (positionMs: Long) -> Unit,
    onInteraction: () -> Unit,
    onSkipActiveSegment: () -> Unit,
    onStayAutoSkip: () -> Unit,
    onPrevEpisode: () -> Unit,
    onNextEpisode: () -> Unit,
    onRateTitle: () -> Unit,
    onToggleQuality: () -> Unit,
    onToggleDubbing: () -> Unit,
    onToggleBalancer: () -> Unit,
    onToggleEpisodes: () -> Unit,
    onTogglePlayerSettings: () -> Unit,
    onToggleAlloha: () -> Unit,
    /** Ряд управления остаётся в композиции (держит фокус), но невидим, пока поверх него лента серий. */
    hidden: Boolean = false,
) {
    val progressDownFocusRequester = when {
        playback.dubbingNames.size > 1 -> focus.dubbing
        playback.balancerNames.size > 1 -> focus.balancer
        qualityCount > 0 -> focus.quality
        showAllohaButton -> focus.alloha
        else -> focus.playerSettings
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
                .graphicsLayer { alpha = if (hidden) 0f else 1f }
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
                    // Подсказка «Серии» сама занимает низ, поэтому нижний отступ при ней меньше.
                    .padding(
                        start = 48.dp,
                        end = 48.dp,
                        top = 20.dp,
                        bottom = if (playback.episodes.isNotEmpty()) 6.dp else 20.dp,
                    ),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                if (activeSkip != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End),
                    ) {
                        val showStay = autoSkipRemainingSeconds != null
                        if (showStay) {
                            TvControlButton(
                                onClick = {
                                    // Кнопка исчезнет — уводим фокус на «Пропустить», иначе он упадёт в меню.
                                    runCatching { focus.skip.requestFocus() }
                                    onStayAutoSkip()
                                },
                                onFocused = onInteraction,
                                focusRequester = focus.stay,
                                modifier = Modifier.focusProperties {
                                    down = focus.play
                                    right = focus.skip
                                },
                            ) { color ->
                                Text(
                                    text = stringResource(R.string.player_stay),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = color,
                                )
                            }
                        }
                        TvControlButton(
                            onClick = onSkipActiveSegment,
                            onFocused = onInteraction,
                            focusRequester = focus.skip,
                            modifier = Modifier.focusProperties {
                                down = focus.play
                                if (showStay) left = focus.stay
                            },
                            primary = highlightedSkipKey == activeSkip.key,
                            fillProgress = autoSkipProgress,
                        ) { color ->
                            Text(
                                text = if (autoSkipRemainingSeconds != null) {
                                    stringResource(
                                        R.string.player_skip_segment_countdown,
                                        autoSkipRemainingSeconds,
                                    )
                                } else {
                                    stringResource(R.string.player_skip_segment)
                                },
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
                    bufferedProgress = progress.bufferedProgress,
                    currentPosition = progress.currentPosition,
                    openingStartMs = playback.activeSkips.opening?.startMs
                        ?.takeIf { showOpeningOnTimeline },
                    openingEndMs = playback.activeSkips.opening?.endMs
                        ?.takeIf { showOpeningOnTimeline },
                    playFocusRequester = focus.play,
                    playUpFocusRequester = focus.skip.takeIf { activeSkip != null },
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
                    finalEpisodeAction = playback.finalEpisodeAction,
                    qualityCount = qualityCount,
                    showEpisodesButton = playback.episodes.isNotEmpty(),
                    allDubbingNames = playback.dubbingNames,
                    currentDubbingIndex = playback.currentDubbingIndex,
                    allBalancerNames = playback.balancerNames,
                    currentBalancerIndex = playback.currentBalancerIndex,
                    playerName = playback.activeBalancerName,
                    dubbing = playback.activeDubbing,
                    currentQualityLabel = currentQualityLabel,
                    adaptiveQuality = adaptiveQuality,
                    qualityFocusRequester = focus.quality,
                    dubbingFocusRequester = focus.dubbing,
                    balancerFocusRequester = focus.balancer,
                    episodesFocusRequester = focus.episodes,
                    upFocusRequester = focus.progress,
                    onInteraction = onInteraction,
                    onPrevEpisode = onPrevEpisode,
                    onNextEpisode = onNextEpisode,
                    onRateTitle = onRateTitle,
                    onToggleQuality = onToggleQuality,
                    onToggleDubbing = onToggleDubbing,
                    onToggleBalancer = onToggleBalancer,
                    onToggleEpisodes = onToggleEpisodes,
                    playerSettingsFocusRequester = focus.playerSettings,
                    onTogglePlayerSettings = onTogglePlayerSettings,
                    allohaFocusRequester = focus.alloha,
                    showAllohaButton = showAllohaButton,
                    nextEpisodeFocusRequester = focus.nextEpisodeControl,
                    onToggleAlloha = onToggleAlloha,
                )
                if (playback.episodes.isNotEmpty()) {
                    TvPlayerEpisodesHint()
                }
            }
        }
    }
}

/** Подсказка под рядом кнопок: стрелка вниз раскрывает ленту серий. */
@Composable
private fun TvPlayerEpisodesHint() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Filled.KeyboardArrowDown,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.7f),
            modifier = Modifier.size(20.dp),
        )
        Text(
            text = stringResource(R.string.player_episodes_list_title),
            style = MaterialTheme.typography.labelMedium,
            color = Color.White.copy(alpha = 0.7f),
        )
    }
}
