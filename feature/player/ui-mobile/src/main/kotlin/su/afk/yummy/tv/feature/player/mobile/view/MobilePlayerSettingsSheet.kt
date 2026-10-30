package su.afk.yummy.tv.feature.player.mobile.view

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import su.afk.yummy.tv.core.designsystem.baseScreen.BaseBottomSheetCustom
import su.afk.yummy.tv.core.designsystem.baseScreen.HideSheetWindowSystemBars
import su.afk.yummy.tv.core.designsystem.components.ScrollToTopOnChange
import su.afk.yummy.tv.core.designsystem.theme.YummySemanticColors
import su.afk.yummy.tv.core.designsystem.utils.balancerFallbackIcon
import su.afk.yummy.tv.core.designsystem.utils.balancerLogoRes
import su.afk.yummy.tv.core.designsystem.utils.dubbingLogoRes
import su.afk.yummy.tv.feature.player.mobile.model.MobilePlayerTrackSettingsTab
import su.afk.yummy.tv.feature.player.model.PlayerEpisodeItem
import su.afk.yummy.tv.feature.player.model.PlayerEpisodeProgress
import su.afk.yummy.tv.feature.player.presentation.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MobilePlayerSettingsSheet(
    dubbingNames: List<String>,
    dubbingEpisodeCounts: List<Int>,
    dubbingViews: List<Int>,
    dubbingSourceNames: List<String>,
    dubbingAvailability: List<Boolean>,
    selectedDubbingIndex: Int,
    onDubbingSelected: (Int) -> Unit,
    balancerNames: List<String>,
    balancerAvailability: List<Boolean>,
    balancerExternal: List<Boolean>,
    selectedBalancerIndex: Int,
    onBalancerSelected: (Int) -> Unit,
    episodes: List<PlayerEpisodeItem>,
    selectedEpisodeIndex: Int,
    onEpisodeSelected: (Int) -> Unit,
    onDismiss: () -> Unit,
    initialTrackTab: MobilePlayerTrackSettingsTab = MobilePlayerTrackSettingsTab.Dubbing,
    onTrackTabChanged: (MobilePlayerTrackSettingsTab) -> Unit = {},
) {
    val trackTabs = MobilePlayerTrackSettingsTab.entries.filter {
        it != MobilePlayerTrackSettingsTab.Episodes || episodes.isNotEmpty()
    }
    val startTab = initialTrackTab
    val trackPagerState = rememberPagerState(
        initialPage = trackTabs.indexOf(startTab).coerceAtLeast(0),
        pageCount = { trackTabs.size },
    )
    val scope = rememberCoroutineScope()

    LaunchedEffect(trackPagerState, trackTabs) {
        snapshotFlow { trackPagerState.settledPage }
            // Первое значение - стартовая страница: если это откат с недоступного таба, он не
            // должен затирать запомненный выбор. Наверх уходят только переключения юзера.
            .drop(1)
            .collect { page -> trackTabs.getOrNull(page)?.let(onTrackTabChanged) }
    }

    BaseBottomSheetCustom(onDismissRequest = onDismiss) { maxHeight ->
        HideSheetWindowSystemBars()

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(maxHeight)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(start = 16.dp, end = 16.dp, bottom = 18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                text = stringResource(R.string.player_mobile_track_settings_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 2.dp),
            )
            val dubbingLabel = stringResource(R.string.player_dubbing_title)
            val playerLabel = stringResource(R.string.player_name_prefix)
            val episodesLabel = stringResource(R.string.player_episode)
            MobilePlayerTrackSettingsTabs(
                tabs = trackTabs,
                selectedTab = trackTabs[trackPagerState.currentPage],
                labelFor = { tab ->
                    when (tab) {
                        MobilePlayerTrackSettingsTab.Dubbing -> dubbingLabel
                        MobilePlayerTrackSettingsTab.Player -> playerLabel
                        MobilePlayerTrackSettingsTab.Episodes -> episodesLabel
                    }
                },
                onTabSelected = { tab ->
                    scope.launch { trackPagerState.animateScrollToPage(trackTabs.indexOf(tab)) }
                },
            )
            HorizontalPager(
                state = trackPagerState,
                verticalAlignment = Alignment.Top,
                pageSpacing = 16.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            ) { page ->
                val listState = rememberLazyListState()
                if (trackTabs[page] == MobilePlayerTrackSettingsTab.Episodes) {
                    // Список серий открываем на текущей, а не с начала: в длинных тайтлах её иначе не видно.
                    LaunchedEffect(selectedEpisodeIndex) {
                        listState.scrollToItem(selectedEpisodeIndex.coerceAtLeast(0))
                    }
                } else {
                    ScrollToTopOnChange(
                        order = if (trackTabs[page] == MobilePlayerTrackSettingsTab.Dubbing) dubbingNames else balancerNames,
                        state = listState,
                    )
                }
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    when (trackTabs[page]) {
                        MobilePlayerTrackSettingsTab.Dubbing -> {
                            item(key = "title") {
                                MobilePlayerSettingsSection(title = stringResource(R.string.player_dubbing_title)) {}
                            }
                            itemsIndexed(dubbingNames, key = { index, name -> "$index-$name" }) { index, name ->
                                val enabled = dubbingAvailability.getOrElse(index) { true }
                                MobilePlayerSelectionRow(
                                    label = name,
                                    selected = index == selectedDubbingIndex,
                                    enabled = enabled,
                                    accentLabel = true,
                                    logoRes = dubbingLogoRes(name),
                                    logoFallbackIcon = Icons.Filled.Mic,
                                    metaContent = { contentColor ->
                                        if (enabled) {
                                            MobilePlayerDubbingMeta(
                                                views = dubbingViews.getOrElse(index) { 0 },
                                                episodeCount = dubbingEpisodeCounts.getOrElse(index) { 0 },
                                                sourceNames = dubbingSourceNames.getOrElse(index) { "" },
                                                contentColor = contentColor,
                                            )
                                        } else {
                                            Text(
                                                text = stringResource(R.string.player_episode_unavailable),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = contentColor.copy(alpha = 0.68f),
                                            )
                                        }
                                    },
                                    onClick = { onDubbingSelected(index) },
                                )
                            }
                        }

                        MobilePlayerTrackSettingsTab.Player -> {
                            item(key = "title") {
                                MobilePlayerSettingsSection(title = stringResource(R.string.player_name_prefix)) {}
                            }
                            itemsIndexed(balancerNames, key = { index, name -> "$index-$name" }) { index, name ->
                                val enabled = balancerAvailability.getOrElse(index) { true }
                                MobilePlayerSelectionRow(
                                    label = name,
                                    selected = index == selectedBalancerIndex,
                                    enabled = enabled,
                                    accentLabel = true,
                                    logoRes = balancerLogoRes(name),
                                    logoFallbackIcon = balancerFallbackIcon(name),
                                    metaContent = { contentColor ->
                                        Text(
                                            text = stringResource(
                                                if (!enabled) {
                                                    R.string.player_episode_unavailable
                                                } else if (balancerExternal.getOrElse(index) { false }) {
                                                    R.string.player_balancer_external_meta
                                                } else {
                                                    R.string.player_balancer_meta
                                                },
                                            ),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (enabled) contentColor else contentColor.copy(alpha = 0.68f),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    },
                                    onClick = { onBalancerSelected(index) },
                                )
                            }
                        }

                        MobilePlayerTrackSettingsTab.Episodes -> {
                            itemsIndexed(episodes, key = { _, item -> item.index }) { _, item ->
                                MobilePlayerSelectionRow(
                                    label = stringResource(R.string.player_episode_number, item.number),
                                    selected = item.index == selectedEpisodeIndex,
                                    metaContent = { contentColor ->
                                        val label = when (val progress = item.progress) {
                                            PlayerEpisodeProgress.None -> null
                                            is PlayerEpisodeProgress.InProgress -> progress.label
                                            is PlayerEpisodeProgress.Watched -> progress.label
                                        }
                                        if (label != null) {
                                            Text(
                                                text = label,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = YummySemanticColors.InProgress,
                                                maxLines = 1,
                                            )
                                        }
                                    },
                                    trailingContent = if (item.progress is PlayerEpisodeProgress.Watched) {
                                        {
                                            // Как значок «просмотрено» в списке эпизодов: белый глаз на тёмном кружке.
                                            Box(
                                                modifier = Modifier
                                                    .size(24.dp)
                                                    .background(Color.Black.copy(alpha = 0.72f), CircleShape),
                                                contentAlignment = Alignment.Center,
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Filled.VisibilityOff,
                                                    contentDescription = null,
                                                    tint = Color.White,
                                                    modifier = Modifier.size(18.dp),
                                                )
                                            }
                                        }
                                    } else {
                                        null
                                    },
                                    onClick = { onEpisodeSelected(item.index) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
