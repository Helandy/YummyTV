package su.afk.yummy.tv.feature.details.mobile.episodes.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.core.designsystem.baseScreen.BaseBottomSheet
import su.afk.yummy.tv.feature.details.episodes.EpisodesState
import su.afk.yummy.tv.feature.details.mobile.R

@Composable
@OptIn(ExperimentalMaterial3Api::class)
internal fun EpisodeDownloadedActionSheet(
    action: EpisodesState.DownloadedEpisodeAction,
    onPlay: () -> Unit,
    onRedownloadDubbing: () -> Unit,
    onOpenDownloads: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    var showDeleteConfirmation by rememberSaveable(action.downloadId) { mutableStateOf(false) }

    BaseBottomSheet(
        onDismissRequest = onDismiss,
        title = stringResource(
            R.string.details_mobile_downloaded_episode_actions_title,
            action.episode,
        ),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        scrollableContent = true,
    ) {
        EpisodeDownloadedInfoCard(action)
        EpisodeSheetActionButton(
            text = stringResource(R.string.details_mobile_play_downloaded_episode),
            icon = Icons.Filled.PlayArrow,
            onClick = onPlay,
            color = MaterialTheme.colorScheme.primary,
        )
        EpisodeSheetActionButton(
            text = stringResource(R.string.details_mobile_episode_open_downloads_action),
            icon = Icons.Filled.Storage,
            onClick = onOpenDownloads,
        )
        if (action.hasAlternativeDubbings) {
            EpisodeSheetActionButton(
                text = stringResource(R.string.details_mobile_redownload_dubbing),
                icon = Icons.Filled.Refresh,
                onClick = onRedownloadDubbing,
            )
        }
        EpisodeSheetActionButton(
            text = stringResource(R.string.details_mobile_delete_downloaded_episode),
            icon = Icons.Filled.DeleteOutline,
            onClick = { showDeleteConfirmation = true },
            color = MaterialTheme.colorScheme.error,
        )
    }

    if (showDeleteConfirmation) {
        EpisodeDownloadDeleteConfirmationDialog(
            episode = action.episode,
            onConfirm = {
                showDeleteConfirmation = false
                onDelete()
            },
            onDismiss = { showDeleteConfirmation = false },
        )
    }
}
