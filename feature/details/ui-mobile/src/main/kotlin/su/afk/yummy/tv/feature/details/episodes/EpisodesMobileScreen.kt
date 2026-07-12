package su.afk.yummy.tv.feature.details.episodes

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.Flow
import su.afk.yummy.tv.core.designsystem.presenter.baseScreen.BaseScreen
import su.afk.yummy.tv.core.designsystem.presenter.mobile.MobileStateContent
import su.afk.yummy.tv.core.designsystem.presenter.mobile.MobileTopBar
import su.afk.yummy.tv.core.designsystem.presenter.mobile.rememberNotificationPermissionGate
import su.afk.yummy.tv.domain.anime.model.AnimeVideo
import su.afk.yummy.tv.feature.details.details.VideosUiState
import su.afk.yummy.tv.feature.details.details.view.BalancerDialog
import su.afk.yummy.tv.feature.details.episodes.utils.blocksNewDownload
import su.afk.yummy.tv.feature.details.episodes.utils.isDownloadBusy
import su.afk.yummy.tv.feature.details.episodes.utils.mobileWatchStatus
import su.afk.yummy.tv.feature.details.episodes.utils.toMobileEpisodeGroups
import su.afk.yummy.tv.feature.details.episodes.view.EpisodeDownloadedActionSheet
import su.afk.yummy.tv.feature.details.episodes.view.EpisodeMobileCard
import su.afk.yummy.tv.feature.details.mobile.R

private val DownloadResolvingColor = Color(0xFFFFC107)

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun EpisodesMobileScreen(
    state: EpisodesState.State,
    effect: Flow<EpisodesState.Effect>,
    onEvent: (EpisodesState.Event) -> Unit,
) {
    val context = LocalContext.current
    val notificationPermissionGate = rememberNotificationPermissionGate()
    LaunchedEffect(effect, context) {
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
        val content = state.videosState as? VideosUiState.Content
        val episodeGroups = remember(content?.videos) {
            content?.videos.orEmpty().toMobileEpisodeGroups()
        }
        MobileStateContent(
            isLoading = state.videosState is VideosUiState.Loading,
            error = null,
            empty = state.videosState is VideosUiState.Empty,
            emptyText = stringResource(R.string.details_mobile_episodes_empty),
            emptyIcon = Icons.Filled.PlayArrow,
        ) {
            LazyColumn(
                modifier = Modifier.navigationBarsPadding(),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    top = 12.dp,
                    end = 16.dp,
                    bottom = 16.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(episodeGroups, key = { it.episode }) { group ->
                    val downloadKeys = remember(group.videos) {
                        group.videos.map { video ->
                            listOf(video.id.toString(), video.iframeUrl).joinToString("|")
                        }
                    }
                    val downloadStatus = downloadKeys
                        .mapNotNull { state.downloadStatuses[it] }
                        .firstOrNull { it.isDownloadBusy() }
                        ?: downloadKeys
                            .mapNotNull { state.downloadStatuses[it] }
                            .firstOrNull { it.status == EpisodesState.EpisodeDownloadUiStatus.Paused }
                        ?: downloadKeys
                            .mapNotNull { state.downloadStatuses[it] }
                            .firstOrNull { it.status == EpisodesState.EpisodeDownloadUiStatus.Downloaded }
                        ?: downloadKeys
                            .mapNotNull { state.downloadStatuses[it] }
                            .firstOrNull { it.status == EpisodesState.EpisodeDownloadUiStatus.Failed }
                    val downloadResolving = downloadKeys.any { it in state.resolvingDownloadKeys }
                    val downloadAwaitingQualitySelection = state.pendingDownloadQualitySelection
                        ?.let { selection -> group.videos.any { it.id == selection.videoId } }
                        ?: false
                    EpisodeMobileCard(
                        video = group.video,
                        watchStatus = group.videos.mobileWatchStatus(state.watchProgress),
                        kodikIframeUrl = group.kodikIframeUrl,
                        downloadStatus = downloadStatus,
                        downloadResolving = downloadResolving,
                        downloadAwaitingQualitySelection = downloadAwaitingQualitySelection,
                        onInfoClick = {
                            onEvent(EpisodesState.Event.EpisodeDubbingsSelected(group.episode))
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
                        onClick = { onEvent(EpisodesState.Event.VideoSelected(group.video)) },
                    )
                }
            }
        }
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
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun EpisodeDownloadDubbingSheet(
    selection: EpisodesState.EpisodeDownloadDubbingSelection,
    onSelected: (List<AnimeVideo>) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(start = 16.dp, end = 16.dp, bottom = 18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(
                    R.string.details_mobile_download_dubbing_title,
                    selection.episode,
                ),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 4.dp),
            )
            if (selection.options.isEmpty()) {
                Text(
                    text = stringResource(
                        if (selection.hasAlternativeDubbings) {
                            R.string.details_mobile_download_other_dubbing_empty
                        } else {
                            R.string.details_mobile_download_dubbing_empty
                        }
                    ),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 8.dp),
                )
            } else {
                LazyColumn(
                    modifier = Modifier.heightIn(max = 420.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    items(selection.options, key = { it.title }) { option ->
                        val busy = option.resolving || option.status.isDownloadBusy()
                        TextButton(
                            enabled = !option.resolving && !option.status.blocksNewDownload(),
                            onClick = { onSelected(option.videos) },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = option.title, modifier = Modifier.fillMaxWidth())
                                    option.subtitle?.let { subtitle ->
                                        Text(
                                            text = subtitle,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.fillMaxWidth(),
                                        )
                                    }
                                }
                                EpisodeDownloadStatusIcon(
                                    status = option.status,
                                    resolving = option.resolving,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun EpisodeDownloadBalancerSheet(
    selection: EpisodesState.EpisodeDownloadBalancerSelection,
    onSelected: (AnimeVideo) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(start = 16.dp, end = 16.dp, bottom = 18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(
                    R.string.details_mobile_download_balancer_title,
                    selection.episode,
                    selection.dubbing,
                ),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 4.dp),
            )
            if (selection.options.isEmpty()) {
                Text(
                    text = stringResource(R.string.details_mobile_download_balancer_empty),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 8.dp),
                )
            } else {
                LazyColumn(
                    modifier = Modifier.heightIn(max = 420.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    items(
                        selection.options,
                        key = { "${it.video.id}|${it.video.iframeUrl}" },
                    ) { option ->
                        val busy = option.resolving || option.status.isDownloadBusy()
                        TextButton(
                            enabled = !option.resolving && !option.status.blocksNewDownload(),
                            onClick = { onSelected(option.video) },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = option.title, modifier = Modifier.fillMaxWidth())
                                    option.subtitle?.let { subtitle ->
                                        Text(
                                            text = subtitle,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.fillMaxWidth(),
                                        )
                                    }
                                }
                                EpisodeDownloadStatusIcon(
                                    status = option.status,
                                    resolving = option.resolving,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EpisodeDownloadStatusIcon(
    status: EpisodesState.EpisodeDownloadUiState?,
    resolving: Boolean,
) {
    when {
        resolving -> Icon(
            imageVector = Icons.Filled.HourglassEmpty,
            contentDescription = null,
            tint = DownloadResolvingColor,
        )

        status.isDownloadBusy() -> CircularProgressIndicator(
            progress = { status?.progress ?: 0f },
            strokeWidth = 2.dp,
            modifier = Modifier.size(22.dp),
        )

        status?.status == EpisodesState.EpisodeDownloadUiStatus.Downloaded ->
            Icon(Icons.Filled.Storage, contentDescription = null)

        status?.status == EpisodesState.EpisodeDownloadUiStatus.Failed ->
            Icon(Icons.Filled.ErrorOutline, contentDescription = null)

        else -> Icon(Icons.Filled.Download, contentDescription = null)
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun EpisodeDownloadQualitySheet(
    selection: EpisodesState.EpisodeDownloadQualitySelection,
    onSelected: (EpisodesState.EpisodeDownloadQualityOption) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(start = 16.dp, end = 16.dp, bottom = 18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(R.string.details_mobile_download_quality_title),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 4.dp),
            )
            Text(
                text = stringResource(R.string.details_mobile_download_quality_prompt),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 4.dp),
            )
            LazyColumn(
                modifier = Modifier.heightIn(max = 420.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                items(selection.options, key = { "${it.label}|${it.url}" }) { option ->
                    TextButton(
                        onClick = { onSelected(option) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = option.label,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        }
    }
}
