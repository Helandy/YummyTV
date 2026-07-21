package su.afk.yummy.tv.feature.player.mobile.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import su.afk.yummy.tv.feature.player.mobile.model.MobilePlayerSettingsMode
import su.afk.yummy.tv.feature.player.mobile.model.MobilePlayerTrackSettingsTab
import su.afk.yummy.tv.feature.player.mobile.R as UiR
import su.afk.yummy.tv.feature.player.presentation.R as PresentationR

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MobilePlayerSettingsSheet(
    mode: MobilePlayerSettingsMode,
    qualities: List<String>,
    selectedQuality: String?,
    onQualitySelected: (String) -> Unit,
    speeds: List<Float>,
    selectedSpeed: Float,
    onSpeedSelected: (Float) -> Unit,
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
    onDismiss: () -> Unit,
    initialTrackTab: MobilePlayerTrackSettingsTab = MobilePlayerTrackSettingsTab.Dubbing,
) {
    val trackTabs = MobilePlayerTrackSettingsTab.entries
    val trackPagerState = rememberPagerState(
        initialPage = trackTabs.indexOf(initialTrackTab).coerceAtLeast(0),
        pageCount = { trackTabs.size },
    )
    val scope = rememberCoroutineScope()

    ModalBottomSheet(onDismissRequest = onDismiss) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
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
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        if (qualities.isNotEmpty()) {
                            MobilePlayerSettingsSection(
                                title = stringResource(UiR.string.player_mobile_quality),
                                modifier = Modifier.weight(1f),
                            ) {
                                qualities.forEach { quality ->
                                    MobilePlayerSelectionRow(
                                        label = quality,
                                        selected = quality == selectedQuality,
                                        onClick = { onQualitySelected(quality) },
                                    )
                                }
                            }
                        }
                        MobilePlayerSettingsSection(
                            title = stringResource(UiR.string.player_mobile_speed),
                            modifier = Modifier.weight(1f),
                        ) {
                            speeds.forEach { speed ->
                                val label = "${speed}x"
                                MobilePlayerSelectionRow(
                                    label = label,
                                    selected = speed == selectedSpeed,
                                    onClick = { onSpeedSelected(speed) },
                                )
                            }
                        }
                    }
                }
            }
            if (mode == MobilePlayerSettingsMode.Track) {
                item {
                    MobilePlayerTrackSettingsTabs(
                        selectedTab = trackTabs[trackPagerState.currentPage],
                        dubbingLabel = stringResource(UiR.string.player_mobile_dubbing),
                        playerLabel = stringResource(UiR.string.player_mobile_player),
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
                        }
                    }
                }
            }
        }
    }
}
