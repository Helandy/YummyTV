package su.afk.yummy.tv.feature.player.view.player

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.core.model.settings.PlayerResizeMode
import su.afk.yummy.tv.core.model.settings.PlayerZoomLevel
import su.afk.yummy.tv.feature.player.common.utils.formatCompactCount
import su.afk.yummy.tv.feature.player.model.PanelReturnFocusTarget
import su.afk.yummy.tv.feature.player.model.PlayerPlaybackUiState
import su.afk.yummy.tv.feature.player.model.TvPlayerFocusRequesters
import su.afk.yummy.tv.feature.player.model.TvPlayerPanel
import su.afk.yummy.tv.feature.player.model.TvPlayerPanelsState
import su.afk.yummy.tv.feature.player.presentation.R
import su.afk.yummy.tv.feature.player.utils.speedLabel

/** Пять панелей выбора: качество, озвучка, скорость, resize/zoom и балансер. */
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
    onAudioTrackSelected: (index: Int) -> Unit,
    onSubtitleTrackSelected: (index: Int) -> Unit,
    onExitPanelDown: (PanelReturnFocusTarget) -> Unit,
) {
    val resizeModes = PlayerResizeMode.entries.toList()
    val zoomLevels = PlayerZoomLevel.entries.toList()

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

    TvPlayerSelectionPanel(
        visible = panels.isOpen(TvPlayerPanel.Dubbing),
        title = stringResource(R.string.player_dubbing_title),
        items = playback.dubbingNames,
        selectedIndex = playback.currentDubbingIndex,
        selectedFocusRequester = focus.selectedDubbing,
        enabledItems = playback.dubbingAvailability,
        accentLabel = true,
        disabledItemMeta = stringResource(R.string.player_episode_unavailable),
        modifier = Modifier
            .align(Alignment.BottomStart)
            .padding(start = 48.dp, bottom = 72.dp),
        itemMetaContent = { idx, contentColor ->
            val views = playback.dubbingViews.getOrElse(idx) { 0 }
            val episodeCount = playback.dubbingEpisodeCounts.getOrElse(idx) { 0 }
            TvDubbingMetaRow(
                views = views.formatCompactCount(),
                episodeCount = episodeCount,
                sourceNames = playback.dubbingSourceNames.getOrElse(idx) { "" },
                contentColor = contentColor,
            )
        },
        onItemSelected = onDubbingSelected,
        onExitDown = { onExitPanelDown(PanelReturnFocusTarget.Dubbing) },
    )

    TvPlayerSelectionPanel(
        visible = panels.isOpen(TvPlayerPanel.Speed),
        title = stringResource(R.string.player_speed_title),
        items = speeds.map { it.speedLabel() },
        selectedIndex = speeds.indexOf(activeSpeed).coerceAtLeast(0),
        selectedFocusRequester = focus.selectedSpeed,
        modifier = Modifier
            .align(Alignment.BottomEnd)
            .padding(end = 48.dp, bottom = 72.dp),
        itemMeta = { stringResource(R.string.player_speed_meta) },
        onItemSelected = onSpeedSelected,
        onExitDown = { onExitPanelDown(PanelReturnFocusTarget.Speed) },
    )

    TvPlayerResizeSettingsPanel(
        visible = panels.isOpen(TvPlayerPanel.Resize),
        resizeModes = resizeModes,
        selectedResizeMode = resizeMode,
        zoomLevels = zoomLevels,
        selectedZoomLevel = zoomLevel,
        selectedResizeFocusRequester = focus.selectedResize,
        modifier = Modifier
            .align(Alignment.BottomEnd)
            .padding(end = 48.dp, bottom = 72.dp),
        onResizeModeSelected = onResizeModeSelected,
        onZoomLevelSelected = onZoomLevelSelected,
        onExitDown = { onExitPanelDown(PanelReturnFocusTarget.Resize) },
    )

    TvPlayerSelectionPanel(
        visible = panels.isOpen(TvPlayerPanel.Balancer),
        title = stringResource(R.string.player_balancer_title),
        items = playback.balancerNames.map {
            it.removePrefix(stringResource(R.string.player_name_prefix))
        },
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

    TvPlayerVolumePanel(
        visible = panels.isOpen(TvPlayerPanel.Volume),
        percent = volumePercent,
        focusRequester = focus.selectedVolume,
        onPercentChange = onVolumeChange,
        onExitDown = { onExitPanelDown(PanelReturnFocusTarget.Volume) },
        modifier = Modifier
            .align(Alignment.BottomEnd)
            .padding(end = 48.dp, bottom = 72.dp),
    )

    TvPlayerAllohaPanel(
        visible = panels.isOpen(TvPlayerPanel.Alloha),
        audioTrackNames = audioTrackNames,
        selectedAudioTrackIndex = selectedAudioTrackIndex,
        onAudioTrackSelected = onAudioTrackSelected,
        subtitleTrackNames = subtitleTrackNames,
        selectedSubtitleTrackIndex = selectedSubtitleTrackIndex,
        onSubtitleTrackSelected = onSubtitleTrackSelected,
        selectedFocusRequester = focus.selectedAlloha,
        modifier = Modifier
            .align(Alignment.BottomEnd)
            .padding(end = 48.dp, bottom = 72.dp),
        onExitDown = { onExitPanelDown(PanelReturnFocusTarget.Alloha) },
    )
}
