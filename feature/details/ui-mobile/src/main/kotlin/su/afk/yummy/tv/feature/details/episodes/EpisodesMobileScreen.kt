package su.afk.yummy.tv.feature.details.episodes

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ErrorOutline
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.Flow
import su.afk.yummy.tv.core.designsystem.presenter.baseScreen.BaseScreen
import su.afk.yummy.tv.core.designsystem.presenter.mobile.MobileStateContent
import su.afk.yummy.tv.core.designsystem.presenter.mobile.MobileTopBar
import su.afk.yummy.tv.domain.anime.model.AnimeVideo
import su.afk.yummy.tv.feature.details.details.VideosUiState
import su.afk.yummy.tv.feature.details.details.view.BalancerDialog
import su.afk.yummy.tv.feature.details.episodes.utils.mobileWatchStatus
import su.afk.yummy.tv.feature.details.episodes.utils.toMobileEpisodeGroups
import su.afk.yummy.tv.feature.details.episodes.view.EpisodeMobileCard
import su.afk.yummy.tv.feature.details.mobile.R

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun EpisodesMobileScreen(

    state: EpisodesState.State,
    effect: Flow<EpisodesState.Effect>,
    onEvent: (EpisodesState.Event) -> Unit,

    ) {
    val context = LocalContext.current
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
                        .firstOrNull {
                            it.status == EpisodesState.EpisodeDownloadUiStatus.Queued ||
                                    it.status == EpisodesState.EpisodeDownloadUiStatus.Downloading
                        }
                        ?: downloadKeys
                            .mapNotNull { state.downloadStatuses[it] }
                            .firstOrNull { it.status == EpisodesState.EpisodeDownloadUiStatus.Downloaded }
                        ?: downloadKeys
                            .mapNotNull { state.downloadStatuses[it] }
                            .firstOrNull { it.status == EpisodesState.EpisodeDownloadUiStatus.Failed }
                    val downloadResolving = downloadKeys.any { it in state.resolvingDownloadKeys }
                    EpisodeMobileCard(
                        video = group.video,
                        watchStatus = group.videos.mobileWatchStatus(state.watchProgress),
                        kodikIframeUrl = group.kodikIframeUrl,
                        downloadStatus = downloadStatus,
                        downloadResolving = downloadResolving,
                        onInfoClick = {
                            onEvent(EpisodesState.Event.EpisodeDubbingsSelected(group.episode))
                        },
                        onDownloadClick = {
                            onEvent(EpisodesState.Event.EpisodeDownloadSelected(group.videos))
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

    state.pendingDownloadQualitySelection?.let { selection ->
        EpisodeDownloadQualitySheet(
            selection = selection,
            onSelected = { onEvent(EpisodesState.Event.DownloadQualitySelected(it)) },
            onDismiss = { onEvent(EpisodesState.Event.DownloadQualityPickerDismissed) },
        )
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun EpisodeDownloadDubbingSheet(
    selection: EpisodesState.EpisodeDownloadDubbingSelection,
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
                    R.string.details_mobile_download_dubbing_title,
                    selection.episode
                ),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 4.dp),
            )
            if (selection.options.isEmpty()) {
                Text(
                    text = stringResource(R.string.details_mobile_download_dubbing_empty),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 8.dp),
                )
            } else {
                selection.options.forEach { option ->
                    val busy = option.resolving ||
                            option.status?.status == EpisodesState.EpisodeDownloadUiStatus.Queued ||
                            option.status?.status == EpisodesState.EpisodeDownloadUiStatus.Downloading
                    TextButton(
                        enabled = !busy && option.status?.status != EpisodesState.EpisodeDownloadUiStatus.Downloaded,
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

@Composable
private fun EpisodeDownloadStatusIcon(
    status: EpisodesState.EpisodeDownloadUiState?,
    resolving: Boolean,
) {
    val busy = resolving ||
            status?.status == EpisodesState.EpisodeDownloadUiStatus.Queued ||
            status?.status == EpisodesState.EpisodeDownloadUiStatus.Downloading
    when {
        busy -> CircularProgressIndicator(
            progress = { status?.progress ?: 0f },
            strokeWidth = 2.dp,
            modifier = Modifier.size(22.dp),
        )

        status?.status == EpisodesState.EpisodeDownloadUiStatus.Downloaded ->
            Icon(Icons.Filled.Done, contentDescription = null)

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
            selection.options.forEach { option ->
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
