package su.afk.yummy.tv.feature.player.view.player

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.core.model.settings.PlayerResizeMode
import su.afk.yummy.tv.core.model.settings.PlayerSubtitleStyleSettings
import su.afk.yummy.tv.core.model.settings.PlayerZoomLevel
import su.afk.yummy.tv.feature.player.model.PanelReturnFocusTarget
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
    audioTrackNames: List<String>,
    selectedAudioTrackIndex: Int,
    subtitleTrackNames: List<String>,
    selectedSubtitleTrackIndex: Int,
    onQualitySelected: (index: Int) -> Unit,
    onDubbingSelected: (index: Int) -> Unit,
    onBalancerSelected: (index: Int) -> Unit,
    onSpeedSelected: (index: Int) -> Unit,
    onResizeModeSelected: (PlayerResizeMode) -> Unit,
    onZoomLevelSelected: (PlayerZoomLevel) -> Unit,
    onVolumeChange: (Int) -> Unit,
    onSubtitleStyleChange: (PlayerSubtitleStyleSettings) -> Unit,
    onStabilizationChange: (Boolean) -> Unit,
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
        disabledItemMeta = stringResource(R.string.player_episode_unavailable),
        modifier = Modifier
            .align(Alignment.BottomStart)
            .padding(start = 48.dp, bottom = 72.dp),
        itemMeta = { stringResource(R.string.player_balancer_meta) },
        onItemSelected = onBalancerSelected,
        onExitDown = { onExitPanelDown(PanelReturnFocusTarget.Balancer) },
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
