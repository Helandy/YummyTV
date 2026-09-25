package su.afk.yummy.tv.feature.details.mobile.episodes

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import su.afk.yummy.tv.core.designsystem.baseScreen.BaseScreen
import su.afk.yummy.tv.core.designsystem.mobile.NotificationPermissionGateHost
import su.afk.yummy.tv.core.designsystem.mobile.bar.MobileTopBar
import su.afk.yummy.tv.core.designsystem.mobile.layout.mobileContentMaxWidth
import su.afk.yummy.tv.core.designsystem.mobile.rememberNotificationPermissionGate
import su.afk.yummy.tv.core.designsystem.mobile.state.MobileStateContent
import su.afk.yummy.tv.core.designsystem.preview.ScreenPreviewTheme
import su.afk.yummy.tv.core.utils.kodik.kodikThumbnailIframeUrl
import su.afk.yummy.tv.feature.details.details.model.VideosUiState
import su.afk.yummy.tv.feature.details.episodes.EpisodesState
import su.afk.yummy.tv.feature.details.mobile.R
import su.afk.yummy.tv.feature.details.mobile.details.view.BalancerDialog
import su.afk.yummy.tv.feature.details.mobile.episodes.utils.mobileWatchStatus
import su.afk.yummy.tv.feature.details.mobile.episodes.utils.representativeVideo
import su.afk.yummy.tv.feature.details.mobile.episodes.view.EpisodeActionsSheet
import su.afk.yummy.tv.feature.details.mobile.episodes.view.EpisodeDownloadBalancerSheet
import su.afk.yummy.tv.feature.details.mobile.episodes.view.EpisodeDownloadDubbingSheet
import su.afk.yummy.tv.feature.details.mobile.episodes.view.EpisodeDownloadQualitySheet
import su.afk.yummy.tv.feature.details.mobile.episodes.view.EpisodeDownloadedActionSheet
import su.afk.yummy.tv.feature.details.mobile.episodes.view.EpisodeDubbingDialog
import su.afk.yummy.tv.feature.details.mobile.episodes.view.EpisodeMobileCard

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
    val notificationPermissionGate = rememberNotificationPermissionGate()
    LaunchedEffect(Unit) {
        effect.collect { event ->
            when (event) {
                is EpisodesState.Effect.ShowToast ->
                    Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    BaseScreen(
        isScroll = false,
        customTopBar = {
            MobileTopBar(
                title = stringResource(R.string.details_mobile_episodes),
                onBack = { onEvent(EpisodesState.Event.BackSelected) },
            )
        },
    ) {
        MobileStateContent(
            isLoading = state.videosState is VideosUiState.Loading,
            error = (state.videosState as? VideosUiState.Error)?.message,
            onRetry = { onEvent(EpisodesState.Event.RetryVideosSelected) },
            empty = state.videosState is VideosUiState.Empty,
            emptyText = stringResource(R.string.details_mobile_episodes_empty),
            emptyIcon = Icons.Filled.PlayArrow,
        ) {
            LazyColumn(
                modifier = Modifier
                    .mobileContentMaxWidth()
                    .navigationBarsPadding(),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    top = 12.dp,
                    end = 16.dp,
                    bottom = 16.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(state.episodeGroups, key = { it.episode }) { group ->
                    val video = group.videos.representativeVideo(state.bestDubbing)
                    val kodikIframeUrl = group.videos.kodikThumbnailIframeUrl(state.bestDubbing)
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
                        watchStatus = group.videos.mobileWatchStatus(state.watchProgress),
                        kodikIframeUrl = kodikIframeUrl,
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
                                    )
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

    state.pendingEpisodeAction?.let { action ->
        EpisodeActionsSheet(
            action = action,
            onToggleWatched = { onEvent(EpisodesState.Event.EpisodeWatchedToggled) },
            onToggleWatchLater = { onEvent(EpisodesState.Event.EpisodeWatchLaterToggled) },
            onDismiss = { onEvent(EpisodesState.Event.EpisodeActionsDismissed) },
        )
    }

    state.pendingEpisodeDubbingSelection?.let { selection ->
        EpisodeDubbingDialog(
            selection = selection,
            onSelected = { onEvent(EpisodesState.Event.EpisodeDubbingSelected(it)) },
            onDismiss = { onEvent(EpisodesState.Event.EpisodeDubbingPickerDismissed) },
        )
    }

    state.pendingBalancerSelection?.let { picker ->
        BalancerDialog(
            picker = picker,
            onConfirmed = { onEvent(EpisodesState.Event.BalancerConfirmed(it)) },
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
            onSelected = { onEvent(EpisodesState.Event.DownloadBalancerSelected(it)) },
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
            onDismiss = { onEvent(EpisodesState.Event.DownloadQualityPickerDismissed) },
        )
    }

    NotificationPermissionGateHost(
        state = notificationPermissionGate,
        permissionWasRequested = state.notificationPermissionRequested,
        onPermissionRequested = { onEvent(EpisodesState.Event.NotificationPermissionRequested) },
    )
}
