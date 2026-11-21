package su.afk.yummy.tv.feature.details.mobile.episodes

import android.widget.Toast
import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.launch
import su.afk.yummy.tv.core.common.episode.episodeGroupKey
import su.afk.yummy.tv.core.designsystem.baseScreen.BaseScreen
import su.afk.yummy.tv.core.designsystem.error.asString
import su.afk.yummy.tv.core.designsystem.mobile.NotificationPermissionGateHost
import su.afk.yummy.tv.core.designsystem.mobile.bar.MobileTopBar
import su.afk.yummy.tv.core.designsystem.mobile.layout.mobileContentMaxWidth
import su.afk.yummy.tv.core.designsystem.mobile.rememberNotificationPermissionGate
import su.afk.yummy.tv.core.designsystem.mobile.state.MobileStateContent
import su.afk.yummy.tv.core.designsystem.motion.rememberStateChangeFade
import su.afk.yummy.tv.core.designsystem.motion.stateChangeFade
import su.afk.yummy.tv.core.designsystem.mvi.collectAsEffect
import su.afk.yummy.tv.core.designsystem.preview.ScreenPreviewTheme
import su.afk.yummy.tv.core.utils.episode.EpisodeThumbnail
import su.afk.yummy.tv.core.utils.kodik.kodikThumbnailIframeUrl
import su.afk.yummy.tv.feature.details.details.model.VideosUiState
import su.afk.yummy.tv.feature.details.episodes.EpisodesState
import su.afk.yummy.tv.feature.details.episodes.utils.resolveVisibleRangeIndex
import su.afk.yummy.tv.feature.details.episodes.utils.watchStatus
import su.afk.yummy.tv.feature.details.mobile.details.view.BalancerDialog
import su.afk.yummy.tv.feature.details.mobile.episodes.utils.representativeVideo
import su.afk.yummy.tv.feature.details.mobile.episodes.view.EpisodeActionsSheet
import su.afk.yummy.tv.feature.details.mobile.episodes.view.EpisodeDownloadBalancerSheet
import su.afk.yummy.tv.feature.details.mobile.episodes.view.EpisodeDownloadDubbingSheet
import su.afk.yummy.tv.feature.details.mobile.episodes.view.EpisodeDownloadQualitySheet
import su.afk.yummy.tv.feature.details.mobile.episodes.view.EpisodeDownloadedActionSheet
import su.afk.yummy.tv.feature.details.mobile.episodes.view.EpisodeDubbingDialog
import su.afk.yummy.tv.feature.details.mobile.episodes.view.EpisodeMobileCard
import su.afk.yummy.tv.feature.details.mobile.episodes.view.EpisodeRangeMobileTabs
import su.afk.yummy.tv.feature.details.mobile.episodes.view.EpisodeWatchFilterAction
import su.afk.yummy.tv.feature.details.mobile.episodes.view.EpisodesMobileSkeleton
import su.afk.yummy.tv.feature.details.presentation.R

private val DownloadResolvingColor = Color(0xFFFFC107)

@Preview(name = "Default", device = "spec:width=412dp,height=915dp,dpi=420", showBackground = true)
@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun EpisodesMobileScreenDefaultPreview() = ScreenPreviewTheme {
    EpisodesMobileScreen(EpisodesState.State(), emptyFlow()) {}
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun EpisodesMobileScreen(
    state: EpisodesState.State,
    effect: Flow<EpisodesState.Effect>,
    onEvent: (EpisodesState.Event) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val episodesListState = rememberLazyListState()
    var pinnedRangeIndex by remember(state.episodeRanges) { mutableStateOf<Int?>(null) }
    LaunchedEffect(episodesListState, state.episodeRanges) {
        episodesListState.interactionSource.interactions.collect { interaction ->
            if (interaction is DragInteraction.Start) {
                pinnedRangeIndex = null
            }
        }
    }
    val selectedRangeIndex by remember(state.episodeRanges, episodesListState) {
        derivedStateOf {
            state.episodeRanges.resolveVisibleRangeIndex(
                firstVisibleItemIndex = episodesListState.firstVisibleItemIndex,
                canScrollForward = episodesListState.canScrollForward,
                canScrollBackward = episodesListState.canScrollBackward,
                pinnedRangeIndex = pinnedRangeIndex,
            )
        }
    }
    val notificationPermissionGate = rememberNotificationPermissionGate()
    effect.collectAsEffect { event ->
        when (event) {
            is EpisodesState.Effect.ShowToast ->
                Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
        }
    }

    BaseScreen(
        isScroll = false,
        customTopBar = {
            MobileTopBar(
                title = stringResource(R.string.details_episodes),
                onBack = { onEvent(EpisodesState.Event.BackSelected) },
                actions = {
                    if (state.videosState is VideosUiState.Content) {
                        EpisodeWatchFilterAction(
                            selected = state.watchFilter,
                            onSelected = { filter ->
                                pinnedRangeIndex = null
                                scope.launch { episodesListState.scrollToItem(0) }
                                onEvent(EpisodesState.Event.WatchFilterSelected(filter))
                            },
                        )
                    }
                },
            )
        },
    ) {
        MobileStateContent(
            isLoading = false,
            error = (state.videosState as? VideosUiState.Error)?.message?.asString(),
            onRetry = { onEvent(EpisodesState.Event.RetryVideosSelected) },
            empty = state.videosState is VideosUiState.Empty,
            emptyText = stringResource(R.string.details_episodes_empty),
            emptyIcon = Icons.Filled.PlayArrow,
        ) {
            val episodesFade = rememberStateChangeFade(state.videosState is VideosUiState.Loading)
            if (state.videosState is VideosUiState.Loading) {
                EpisodesMobileSkeleton(modifier = Modifier.mobileContentMaxWidth())
                return@MobileStateContent
            }
            Column(modifier = Modifier.mobileContentMaxWidth().stateChangeFade(episodesFade)) {
                if (state.episodeRanges.isNotEmpty()) {
                    EpisodeRangeMobileTabs(
                        ranges = state.episodeRanges,
                        selectedRangeIndex = selectedRangeIndex,
                        onRangeSelected = { index, range ->
                            pinnedRangeIndex = index
                            scope.launch { episodesListState.scrollToItem(range.startIndex) }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                if (state.visibleEpisodeGroups.isEmpty()) {
                    Text(
                        text = stringResource(R.string.details_episodes_filter_empty),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                    )
                }
                LazyColumn(
                    state = episodesListState,
                    // Под вкладками диапазонов верхний отступ — padding списка, а не contentPadding:
                    // иначе прокрученные карточки проходят через эти 12dp и просвечивают под вкладками.
                    // Без вкладок наоборот: под шапкой отступ в contentPadding, чтобы карточки уходили
                    // прямо под неё, а не обрезались по полосе фона в 12dp.
                    modifier = Modifier
                        .weight(1f)
                        .padding(top = if (state.episodeRanges.isNotEmpty()) 12.dp else 0.dp)
                        .navigationBarsPadding(),
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        top = if (state.episodeRanges.isNotEmpty()) 0.dp else 12.dp,
                        end = 16.dp,
                        bottom = 16.dp,
                    ),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(state.visibleEpisodeGroups, key = { it.episode }) { group ->
                        val video = group.videos.representativeVideo(state.bestDubbing)
                        val thumbnail = EpisodeThumbnail(
                            iframeUrl = group.videos.kodikThumbnailIframeUrl(state.bestDubbing).orEmpty(),
                            animeId = state.animeId,
                            episode = group.episode,
                        )
                        val downloadStatus = state.resolvedDownloadStatuses[group.episode]
                        val downloadKeys = remember(group.videos) {
                            group.videos.map { video ->
                                listOf(video.id.toString(), video.iframeUrl).joinToString("|")
                            }
                        }
                        val downloadResolving = downloadKeys.any { it in state.resolvingDownloadKeys }
                        val downloadAwaitingQualitySelection = state.pendingDownloadQualitySelection
                            ?.let { selection -> group.videos.any { it.id == selection.videoId } }
                            ?: false
                        val episodeInfo = state.episodeInfo[group.episode]
                        EpisodeMobileCard(
                            video = video,
                            episodeNumber = group.episode,
                            watchStatus = group.videos.watchStatus(state.watchProgress),
                            isInWatchLater = group.episode.episodeGroupKey() in state.watchLaterEpisodes,
                            flags = state.episodeFlags[group.episode.episodeGroupKey()],
                            thumbnail = thumbnail,
                            episodeTitle = episodeInfo?.title,
                            episodeDescription = episodeInfo?.description,
                            descriptionExpanded = group.episode in state.expandedEpisodeDescriptions,
                            downloadStatus = downloadStatus,
                            downloadResolving = downloadResolving,
                            downloadAwaitingQualitySelection = downloadAwaitingQualitySelection,
                            onToggleDescription = {
                                onEvent(EpisodesState.Event.EpisodeDescriptionToggled(group.episode))
                            },
                            onDownloadClick = {
                                if (downloadStatus?.status == EpisodesState.EpisodeDownloadUiStatus.Downloaded) {
                                    onEvent(
                                        EpisodesState.Event.DownloadedEpisodeSelected(
                                            videos = group.videos,
                                            download = downloadStatus,
                                        ),
                                    )
                                } else {
                                    onEvent(EpisodesState.Event.EpisodeDownloadSelected(group.videos))
                                }
                            },
                            onOpenDownloadsClick = {
                                onEvent(EpisodesState.Event.OpenDownloadsScreenSelected)
                            },
                            onClick = { onEvent(EpisodesState.Event.EpisodeSelected(video)) },
                            onLongClick = {
                                onEvent(EpisodesState.Event.EpisodeActionsRequested(group.videos))
                            },
                        )
                    }
                }
            }
        }
    }

    state.pendingEpisodeAction?.let { action ->
        EpisodeActionsSheet(
            action = action,
            onToggleWatched = { onEvent(EpisodesState.Event.EpisodeWatchedToggled) },
            onClearWatched = { onEvent(EpisodesState.Event.EpisodeWatchedCleared) },
            onToggleWatchedUpTo = {
                onEvent(EpisodesState.Event.EpisodesWatchedUpToChanged(watched = !action.isWatched))
            },
            onToggleWatchLater = { onEvent(EpisodesState.Event.EpisodeWatchLaterToggled) },
            onDismiss = { onEvent(EpisodesState.Event.EpisodeActionsDismissed) },
        )
    }

    state.pendingEpisodeDubbingSelection?.let { selection ->
        EpisodeDubbingDialog(
            selection = selection,
            onSelected = { onEvent(EpisodesState.Event.EpisodeDubbingSelected(it)) },
            onStarClick = { onEvent(EpisodesState.Event.DubbingStarToggled(it)) },
            onDismiss = { onEvent(EpisodesState.Event.EpisodeDubbingPickerDismissed) },
        )
    }

    state.pendingBalancerSelection?.let { picker ->
        BalancerDialog(
            picker = picker,
            onConfirmed = { onEvent(EpisodesState.Event.BalancerConfirmed(it)) },
            onStarClick = { onEvent(EpisodesState.Event.BalancerStarToggled(it)) },
            onDismiss = { onEvent(EpisodesState.Event.BalancerPickerDismissed) },
        )
    }

    state.pendingDownloadDubbingSelection?.let { selection ->
        EpisodeDownloadDubbingSheet(
            selection = selection,
            onSelected = { onEvent(EpisodesState.Event.DownloadDubbingSelected(it)) },
            onDismiss = { onEvent(EpisodesState.Event.DownloadDubbingPickerDismissed) },
        )
    }

    state.pendingDownloadedEpisodeAction?.let { action ->
        EpisodeDownloadedActionSheet(
            action = action,
            onPlay = { onEvent(EpisodesState.Event.PlayDownloadedEpisodeSelected) },
            onRedownloadDubbing = {
                onEvent(EpisodesState.Event.RedownloadDubbingSelected)
            },
            onOpenDownloads = { onEvent(EpisodesState.Event.OpenDownloadsScreenSelected) },
            onDelete = { onEvent(EpisodesState.Event.DeleteDownloadedEpisodeSelected) },
            onDismiss = { onEvent(EpisodesState.Event.DownloadedEpisodeActionDismissed) },
        )
    }

    state.pendingDownloadBalancerSelection?.let { selection ->
        EpisodeDownloadBalancerSheet(
            selection = selection,
            onSelected = { video, rememberAsPreset ->
                onEvent(EpisodesState.Event.DownloadBalancerSelected(video, rememberAsPreset))
            },
            onDismiss = { onEvent(EpisodesState.Event.DownloadBalancerPickerDismissed) },
        )
    }

    state.pendingDownloadQualitySelection?.let { selection ->
        EpisodeDownloadQualitySheet(
            selection = selection,
            onSelected = { option ->
                notificationPermissionGate {
                    onEvent(EpisodesState.Event.DownloadQualitySelected(option))
                }
            },
            onPresetChange = { onEvent(EpisodesState.Event.DownloadPresetResetSelected) },
            onDismiss = { onEvent(EpisodesState.Event.DownloadQualityPickerDismissed) },
        )
    }

    NotificationPermissionGateHost(
        state = notificationPermissionGate,
        permissionWasRequested = state.notificationPermissionRequested,
        onPermissionRequested = { onEvent(EpisodesState.Event.NotificationPermissionRequested) },
    )
}
