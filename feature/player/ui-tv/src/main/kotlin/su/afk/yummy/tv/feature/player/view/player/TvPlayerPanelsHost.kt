package su.afk.yummy.tv.feature.player.view.player

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.core.designsystem.theme.YummySemanticColors
import su.afk.yummy.tv.core.designsystem.utils.balancerFallbackIcon
import su.afk.yummy.tv.core.designsystem.utils.balancerLogoRes
import su.afk.yummy.tv.core.model.settings.PlayerResizeMode
import su.afk.yummy.tv.core.model.settings.PlayerSubtitleStyleSettings
import su.afk.yummy.tv.core.model.settings.PlayerZoomLevel
import su.afk.yummy.tv.feature.player.model.PanelReturnFocusTarget
import su.afk.yummy.tv.feature.player.model.PlayerEpisodeProgress
import su.afk.yummy.tv.feature.player.model.PlayerPlaybackUiState
import su.afk.yummy.tv.feature.player.model.TvPlayerFocusRequesters
import su.afk.yummy.tv.feature.player.model.TvPlayerPanel
import su.afk.yummy.tv.feature.player.model.TvPlayerPanelsState
import su.afk.yummy.tv.feature.player.presentation.R

/** Панели выбора (качество, озвучка, балансер, Alloha) и боковое меню настроек плеера справа. */
@Composable
internal fun BoxScope.TvPlayerPanelsHost(
    panels: TvPlayerPanelsState,
    focus: TvPlayerFocusRequesters,
    playback: PlayerPlaybackUiState,
    qualities: List<String>,
    activeQuality: String?,
    speeds: List<Float>,
    activeSpeed: Float,
    resizeMode: PlayerResizeMode,
    zoomLevel: PlayerZoomLevel,
    volumePercent: Int,
    showVolume: Boolean,
    subtitleStyle: PlayerSubtitleStyleSettings,
    showStabilization: Boolean,
    stabilizationEnabled: Boolean,
    showAdaptiveQuality: Boolean,
    adaptiveQualityEnabled: Boolean,
    audioTrackNames: List<String>,
    selectedAudioTrackIndex: Int,
    subtitleTrackNames: List<String>,
    selectedSubtitleTrackIndex: Int,
    onQualitySelected: (index: Int) -> Unit,
    onDubbingSelected: (index: Int) -> Unit,
    onBalancerSelected: (index: Int) -> Unit,
    onEpisodeSelected: (index: Int) -> Unit,
    onSpeedSelected: (index: Int) -> Unit,
    onResizeModeSelected: (PlayerResizeMode) -> Unit,
    onZoomLevelSelected: (PlayerZoomLevel) -> Unit,
    onVolumeChange: (Int) -> Unit,
    onSubtitleStyleChange: (PlayerSubtitleStyleSettings) -> Unit,
    onStabilizationChange: (Boolean) -> Unit,
    onAdaptiveQualityChange: (Boolean) -> Unit,
    onAudioTrackSelected: (index: Int) -> Unit,
    onSubtitleTrackSelected: (index: Int) -> Unit,
    onExitPanelDown: (PanelReturnFocusTarget) -> Unit,
) {
    TvPlayerSelectionPanel(
        visible = panels.isOpen(TvPlayerPanel.Quality),
        title = stringResource(R.string.player_quality_title),
        items = qualities,
        selectedIndex = qualities.indexOf(activeQuality),
        selectedFocusRequester = focus.selectedQuality,
        modifier = Modifier
            .align(Alignment.BottomEnd)
            .padding(end = 48.dp, bottom = 72.dp),
        itemMeta = { stringResource(R.string.player_quality_meta) },
        onItemSelected = onQualitySelected,
        onExitDown = { onExitPanelDown(PanelReturnFocusTarget.Quality) },
    )

    TvDubbingSelectionPanel(
        visible = panels.isOpen(TvPlayerPanel.Dubbing),
        playback = playback,
        selectedFocusRequester = focus.selectedDubbing,
        modifier = Modifier
            .align(Alignment.BottomStart)
            .padding(start = 48.dp, bottom = 72.dp),
        onItemSelected = onDubbingSelected,
        onExitDown = { onExitPanelDown(PanelReturnFocusTarget.Dubbing) },
    )

    TvPlayerSelectionPanel(
        visible = panels.isOpen(TvPlayerPanel.Balancer),
        title = stringResource(R.string.player_name_prefix),
        items = playback.balancerNames,
        selectedIndex = playback.currentBalancerIndex,
        selectedFocusRequester = focus.selectedBalancer,
        enabledItems = playback.balancerAvailability,
        accentLabel = true,
        logoFor = ::balancerLogoRes,
        logoFallbackFor = ::balancerFallbackIcon,
        disabledItemMeta = stringResource(R.string.player_episode_unavailable),
        modifier = Modifier
            .align(Alignment.BottomStart)
            .padding(start = 48.dp, bottom = 72.dp),
        itemMeta = { index ->
            stringResource(
                if (playback.balancerExternal.getOrElse(index) { false }) {
                    R.string.player_balancer_external_meta
                } else {
                    R.string.player_balancer_meta
                },
            )
        },
        onItemSelected = onBalancerSelected,
        onExitDown = { onExitPanelDown(PanelReturnFocusTarget.Balancer) },
    )

    TvPlayerSelectionPanel(
        visible = panels.isOpen(TvPlayerPanel.Episodes),
        title = stringResource(R.string.player_episode),
        items = playback.episodes.map { stringResource(R.string.player_episode_number, it.number) },
        selectedIndex = playback.currentEpisodeIndex,
        selectedFocusRequester = focus.selectedEpisodes,
        modifier = Modifier
            .align(Alignment.BottomEnd)
            .padding(end = 48.dp, bottom = 72.dp),
        itemMetaContent = { index, contentColor ->
            val progress = playback.episodes.getOrNull(index)?.progress
            val label = when (progress) {
                is PlayerEpisodeProgress.InProgress -> progress.label
                is PlayerEpisodeProgress.Watched -> progress.label
                PlayerEpisodeProgress.None, null -> null
            }
            if (label != null) {
                // В фокусе фон оранжевый и зелёный на нём не читается: остаётся цвет строки.
                val color = if (contentColor == MaterialTheme.colorScheme.onPrimary) {
                    contentColor
                } else {
                    YummySemanticColors.InProgress
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (progress is PlayerEpisodeProgress.Watched) {
                        Icon(
                            imageVector = Icons.Filled.VisibilityOff,
                            contentDescription = null,
                            tint = contentColor,
                            modifier = Modifier.size(14.dp),
                        )
                        Spacer(Modifier.width(4.dp))
                    }
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        color = color,
                        maxLines = 1,
                    )
                }
            }
        },
        onItemSelected = onEpisodeSelected,
        onExitDown = { onExitPanelDown(PanelReturnFocusTarget.Episodes) },
    )

    TvPlayerSettingsPanel(
        visible = panels.isOpen(TvPlayerPanel.PlayerSettings),
        entryFocusRequester = focus.selectedPlayerSettings,
        speeds = speeds,
        activeSpeed = activeSpeed,
        onSpeedSelected = onSpeedSelected,
        resizeMode = resizeMode,
        zoomLevel = zoomLevel,
        onResizeModeSelected = onResizeModeSelected,
        onZoomLevelSelected = onZoomLevelSelected,
        subtitleTrackNames = subtitleTrackNames,
        selectedSubtitleTrackIndex = selectedSubtitleTrackIndex,
        onSubtitleTrackSelected = onSubtitleTrackSelected,
        subtitleStyle = subtitleStyle,
        onSubtitleStyleChange = onSubtitleStyleChange,
        showVolume = showVolume,
        volumePercent = volumePercent,
        onVolumeChange = onVolumeChange,
        showStabilization = showStabilization,
        stabilizationEnabled = stabilizationEnabled,
        onStabilizationChange = onStabilizationChange,
        showAdaptiveQuality = showAdaptiveQuality,
        adaptiveQualityEnabled = adaptiveQualityEnabled,
        onAdaptiveQualityChange = onAdaptiveQualityChange,
        onClose = { onExitPanelDown(PanelReturnFocusTarget.PlayerSettings) },
        modifier = Modifier.align(Alignment.CenterEnd),
    )

    TvPlayerAllohaPanel(
        visible = panels.isOpen(TvPlayerPanel.Alloha),
        audioTrackNames = audioTrackNames,
        selectedAudioTrackIndex = selectedAudioTrackIndex,
        onAudioTrackSelected = onAudioTrackSelected,
        selectedFocusRequester = focus.selectedAlloha,
        modifier = Modifier
            .align(Alignment.BottomEnd)
            .padding(end = 48.dp, bottom = 72.dp),
        onExitDown = { onExitPanelDown(PanelReturnFocusTarget.Alloha) },
    )
}
