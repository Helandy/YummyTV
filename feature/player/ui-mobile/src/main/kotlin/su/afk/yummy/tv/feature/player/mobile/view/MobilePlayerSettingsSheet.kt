package su.afk.yummy.tv.feature.player.mobile.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import su.afk.yummy.tv.core.designsystem.baseScreen.BaseBottomSheetCustom
import su.afk.yummy.tv.core.designsystem.baseScreen.HideSheetWindowSystemBars
import su.afk.yummy.tv.core.model.settings.PlayerResizeMode
import su.afk.yummy.tv.feature.player.mobile.model.MobilePlayerSettingsMode
import su.afk.yummy.tv.feature.player.mobile.model.MobilePlayerTrackSettingsTab
import kotlin.math.roundToInt
import su.afk.yummy.tv.feature.player.mobile.R as UiR
import su.afk.yummy.tv.feature.player.presentation.R as PresentationR

private fun PlayerResizeMode.mobileResizeLabelRes(): Int = when (this) {
    PlayerResizeMode.FIT -> PresentationR.string.player_resize_fit
    PlayerResizeMode.ZOOM -> PresentationR.string.player_resize_zoom
    PlayerResizeMode.STRETCH -> PresentationR.string.player_resize_stretch
    PlayerResizeMode.CROP -> PresentationR.string.player_resize_crop
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MobilePlayerSettingsSheet(
    mode: MobilePlayerSettingsMode,
    qualities: List<String>,
    selectedQuality: String?,
    onQualitySelected: (String) -> Unit,
    selectedSpeed: Float,
    onSpeedSelected: (Float) -> Unit,
    resizeModes: List<PlayerResizeMode>,
    selectedResizeMode: PlayerResizeMode,
    onResizeModeSelected: (PlayerResizeMode) -> Unit,
    dubbingNames: List<String>,
    dubbingEpisodeCounts: List<Int>,
    dubbingViews: List<Int>,
    dubbingSourceNames: List<String>,
    dubbingAvailability: List<Boolean>,
    selectedDubbingIndex: Int,
    onDubbingSelected: (Int) -> Unit,
    balancerNames: List<String>,
    balancerAvailability: List<Boolean>,
    selectedBalancerIndex: Int,
    onBalancerSelected: (Int) -> Unit,
    audioTrackNames: List<String>,
    selectedAudioTrackIndex: Int,
    onAudioTrackSelected: (Int) -> Unit,
    showAudioSection: Boolean,
    subtitleTrackNames: List<String>,
    selectedSubtitleTrackIndex: Int,
    onSubtitleTrackSelected: (Int) -> Unit,
    showSubtitleSection: Boolean,
    onDismiss: () -> Unit,
    initialTrackTab: MobilePlayerTrackSettingsTab = MobilePlayerTrackSettingsTab.Dubbing,
) {
    val trackTabs = buildList {
        add(MobilePlayerTrackSettingsTab.Dubbing)
        add(MobilePlayerTrackSettingsTab.Player)
        if (showAudioSection || showSubtitleSection) add(MobilePlayerTrackSettingsTab.Alloha)
    }
    val trackPagerState = rememberPagerState(
        initialPage = trackTabs.indexOf(initialTrackTab).coerceAtLeast(0),
        pageCount = { trackTabs.size },
    )
    val scope = rememberCoroutineScope()

    BaseBottomSheetCustom(onDismissRequest = onDismiss) { maxHeight ->
        HideSheetWindowSystemBars()

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .let {
                    if (mode == MobilePlayerSettingsMode.Track) it.height(maxHeight) else it.heightIn(
                        max = maxHeight
                    )
                }
                .windowInsetsPadding(WindowInsets.navigationBars),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                Text(
                    text = when (mode) {
                        MobilePlayerSettingsMode.Track -> stringResource(UiR.string.player_mobile_track_settings_title)
                        MobilePlayerSettingsMode.Playback -> stringResource(UiR.string.player_mobile_playback_settings_title)
                    },
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 2.dp),
                )
            }
            if (mode == MobilePlayerSettingsMode.Playback) {
                if (qualities.isNotEmpty()) {
                    item {
                        val qualityIndex = qualities.indexOf(selectedQuality).coerceAtLeast(0)
                        MobilePlayerSettingsSection(title = stringResource(UiR.string.player_mobile_quality)) {
                            MobilePlayerSliderRow(
                                valueText = qualities[qualityIndex],
                                value = qualityIndex,
                                valueRange = 0..(qualities.size - 1).coerceAtLeast(0),
                                onValueChange = { index -> onQualitySelected(qualities[index]) },
                                tickLabels = qualities,
                            )
                        }
                    }
                }
                item {
                    val speedTenths = (selectedSpeed * 10).roundToInt().coerceIn(5, 30)
                    MobilePlayerSettingsSection(title = stringResource(UiR.string.player_mobile_speed)) {
                        MobilePlayerSliderRow(
                            valueText = "%.1fx".format(speedTenths / 10f),
                            value = speedTenths,
                            valueRange = 5..30,
                            onValueChange = { tenths -> onSpeedSelected(tenths / 10f) },
                        )
                    }
                }
                item {
                    MobilePlayerSettingsSection(title = stringResource(PresentationR.string.player_resize_title)) {
                        resizeModes.forEach { resizeMode ->
                            MobilePlayerSelectionRow(
                                label = stringResource(resizeMode.mobileResizeLabelRes()),
                                selected = resizeMode == selectedResizeMode,
                                onClick = { onResizeModeSelected(resizeMode) },
                            )
                        }
                    }
                }
            }
            if (mode == MobilePlayerSettingsMode.Track) {
                item {
                    val dubbingLabel = stringResource(UiR.string.player_mobile_dubbing)
                    val playerLabel = stringResource(UiR.string.player_mobile_player)
                    val allohaLabel = stringResource(UiR.string.player_mobile_audio_track)
                    MobilePlayerTrackSettingsTabs(
                        tabs = trackTabs,
                        selectedTab = trackTabs[trackPagerState.currentPage],
                        labelFor = { tab ->
                            when (tab) {
                                MobilePlayerTrackSettingsTab.Dubbing -> dubbingLabel
                                MobilePlayerTrackSettingsTab.Player -> playerLabel
                                MobilePlayerTrackSettingsTab.Alloha -> allohaLabel
                            }
                        },
                        onTabSelected = { tab ->
                            scope.launch { trackPagerState.animateScrollToPage(trackTabs.indexOf(tab)) }
                        },
                    )
                }
                item {
                    HorizontalPager(
                        state = trackPagerState,
                        verticalAlignment = Alignment.Top,
                        pageSpacing = 16.dp,
                        modifier = Modifier.fillMaxWidth(),
                    ) { page ->
                        when (trackTabs[page]) {
                            MobilePlayerTrackSettingsTab.Dubbing ->
                                MobilePlayerSettingsSection(title = stringResource(UiR.string.player_mobile_dubbing)) {
                                    dubbingNames.forEachIndexed { index, name ->
                                        val enabled = dubbingAvailability.getOrElse(index) { true }
                                        MobilePlayerSelectionRow(
                                            label = name,
                                            selected = index == selectedDubbingIndex,
                                            enabled = enabled,
                                            metaContent = { contentColor ->
                                                if (enabled) {
                                                    MobilePlayerDubbingMeta(
                                                        views = dubbingViews.getOrElse(index) { 0 },
                                                        episodeCount = dubbingEpisodeCounts.getOrElse(
                                                            index
                                                        ) { 0 },
                                                        sourceNames = dubbingSourceNames.getOrElse(
                                                            index
                                                        ) { "" },
                                                        contentColor = contentColor,
                                                    )
                                                } else {
                                                    Text(
                                                        text = stringResource(PresentationR.string.player_episode_unavailable),
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = contentColor.copy(alpha = 0.68f),
                                                    )
                                                }
                                            },
                                            onClick = { onDubbingSelected(index) },
                                        )
                                    }
                                }

                            MobilePlayerTrackSettingsTab.Player ->
                                MobilePlayerSettingsSection(title = stringResource(UiR.string.player_mobile_player)) {
                                    balancerNames.forEachIndexed { index, name ->
                                        val enabled = balancerAvailability.getOrElse(index) { true }
                                        MobilePlayerSelectionRow(
                                            label = name,
                                            selected = index == selectedBalancerIndex,
                                            enabled = enabled,
                                            metaContent = { contentColor ->
                                                if (!enabled) {
                                                    Text(
                                                        text = stringResource(PresentationR.string.player_episode_unavailable),
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = contentColor.copy(alpha = 0.68f),
                                                    )
                                                }
                                            },
                                            onClick = { onBalancerSelected(index) },
                                        )
                                    }
                                }

                            // Alloha reports dubbings and subtitles together, so they share one
                            // tab as two sections rather than splitting into sibling tabs.
                            MobilePlayerTrackSettingsTab.Alloha ->
                                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                                    if (showAudioSection) {
                                        MobilePlayerSettingsSection(
                                            title = stringResource(UiR.string.player_mobile_dubbing),
                                        ) {
                                            audioTrackNames.forEachIndexed { index, name ->
                                                MobilePlayerSelectionRow(
                                                    label = name,
                                                    selected = index == selectedAudioTrackIndex,
                                                    onClick = { onAudioTrackSelected(index) },
                                                )
                                            }
                                        }
                                    }
                                    if (showSubtitleSection) {
                                        MobilePlayerSettingsSection(
                                            title = stringResource(UiR.string.player_mobile_subtitles),
                                        ) {
                                            subtitleTrackNames.forEachIndexed { index, name ->
                                                MobilePlayerSelectionRow(
                                                    label = name,
                                                    selected = index == selectedSubtitleTrackIndex,
                                                    onClick = { onSubtitleTrackSelected(index) },
                                                )
                                            }
                                        }
                                    }
                                }
                        }
                    }
                }
            }
        }
    }
}
