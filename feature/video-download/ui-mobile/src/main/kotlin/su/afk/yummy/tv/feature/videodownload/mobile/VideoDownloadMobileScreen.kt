package su.afk.yummy.tv.feature.videodownload.mobile

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SaveAlt
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
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
import su.afk.yummy.tv.core.designsystem.mobile.state.MobileMessage
import su.afk.yummy.tv.core.designsystem.preview.ScreenPreviewTheme
import su.afk.yummy.tv.feature.videodownload.VideoDownloadState
import su.afk.yummy.tv.feature.videodownload.mobile.utils.formatDiskSize
import su.afk.yummy.tv.feature.videodownload.mobile.utils.isEligibleForExport
import su.afk.yummy.tv.feature.videodownload.mobile.view.VideoDownloadDeleteConfirmationDialog
import su.afk.yummy.tv.feature.videodownload.mobile.view.VideoDownloadMobileCard
import su.afk.yummy.tv.feature.videodownload.mobile.view.VideoExportAllConfirmationDialog
import su.afk.yummy.tv.feature.videodownload.mobile.view.VideoExportReExportConfirmationDialog

@Preview(name = "Default", device = "spec:width=412dp,height=915dp,dpi=420", showBackground = true)
@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun VideoDownloadMobileScreenDefaultPreview() =
    ScreenPreviewTheme {
        VideoDownloadMobileScreen(VideoDownloadState.State(), emptyFlow()) {}
    }

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun VideoDownloadMobileScreen(
    state: VideoDownloadState.State,
    effect: Flow<VideoDownloadState.Effect>,
    onEvent: (VideoDownloadState.Event) -> Unit,
) {
    val notificationPermissionGate = rememberNotificationPermissionGate()
    val context = LocalContext.current
    val exportDirectoryPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        uri?.let {
            onEvent(VideoDownloadState.Event.ExportDirectoryGranted(it.toString()))
        }
    }
    LaunchedEffect(Unit) {
        effect.collect { exportEffect ->
            when (exportEffect) {
                VideoDownloadState.Effect.OpenExportDirectoryPicker ->
                    exportDirectoryPicker.launch(null)

                VideoDownloadState.Effect.ExportDirectorySelectionFailed ->
                    Toast.makeText(
                        context,
                        R.string.video_export_directory_error,
                        Toast.LENGTH_LONG,
                    ).show()
            }
        }
    }
    val occupiedSize = state.occupiedBytes.formatDiskSize()
    BaseScreen(
        contentModifier = Modifier.navigationBarsPadding(),
        isScroll = false,
        customTopBar = {
            MobileTopBar(
                title = stringResource(R.string.video_download_mobile_title),
                onBack = { onEvent(VideoDownloadState.Event.BackSelected) },
                actions = {
                    IconButton(
                        onClick = { onEvent(VideoDownloadState.Event.ExportAllSelected) },
                        enabled = state.items.any { item ->
                            item.isEligibleForExport(state.exportDestination?.uri)
                        },
                    ) {
                        Icon(
                            imageVector = Icons.Filled.SaveAlt,
                            contentDescription = stringResource(R.string.video_export_all),
                        )
                    }
                    Text(
                        text = stringResource(
                            R.string.video_download_total_disk_size,
                            occupiedSize,
                        ),
                        modifier = Modifier.padding(end = 16.dp),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                    )
                },
            )
        },
    ) {
        if (state.items.isEmpty()) {
            MobileMessage(
                title = stringResource(R.string.video_download_empty),
                icon = Icons.Outlined.Download,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .mobileContentMaxWidth()
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(state.items, key = { it.id }) { item ->
                    VideoDownloadMobileCard(
                        item = item,
                        onClick = { onEvent(VideoDownloadState.Event.ItemSelected(item.id)) },
                        onDetailsClick = { onEvent(VideoDownloadState.Event.DetailsSelected(item.animeId)) },
                        onDelete = { onEvent(VideoDownloadState.Event.DeleteSelected(item.id)) },
                        onPause = { onEvent(VideoDownloadState.Event.PauseSelected(item.id)) },
                        onResume = {
                            notificationPermissionGate {
                                onEvent(VideoDownloadState.Event.ResumeSelected(item.id))
                            }
                        },
                        onRestart = {
                            notificationPermissionGate {
                                onEvent(VideoDownloadState.Event.RestartSelected(item.id))
                            }
                        },
                        onExport = {
                            notificationPermissionGate {
                                onEvent(VideoDownloadState.Event.ExportSelected(item.id))
                            }
                        },
                        onCancelExport = {
                            onEvent(VideoDownloadState.Event.CancelExportSelected(item.id))
                        },
                    )
                }
            }
        }
    }

    state.pendingDeleteItem?.let { item ->
        VideoDownloadDeleteConfirmationDialog(
            animeTitle = item.animeTitle,
            episode = item.episode,
            onConfirm = { onEvent(VideoDownloadState.Event.DeleteConfirmed) },
            onDismiss = { onEvent(VideoDownloadState.Event.DeleteDismissed) },
        )
    }

    state.pendingReExportItem?.let { item ->
        VideoExportReExportConfirmationDialog(
            animeTitle = item.animeTitle,
            episode = item.episode,
            onConfirm = {
                notificationPermissionGate {
                    onEvent(VideoDownloadState.Event.ReExportConfirmed)
                }
            },
            onDismiss = { onEvent(VideoDownloadState.Event.ReExportDismissed) },
        )
    }

    if (state.pendingBulkExportCount > 0) {
        VideoExportAllConfirmationDialog(
            count = state.pendingBulkExportCount,
            onConfirm = {
                notificationPermissionGate {
                    onEvent(VideoDownloadState.Event.ExportAllConfirmed)
                }
            },
            onDismiss = { onEvent(VideoDownloadState.Event.ExportAllDismissed) },
        )
    }

    NotificationPermissionGateHost(
        state = notificationPermissionGate,
        permissionWasRequested = state.notificationPermissionRequested,
        onPermissionRequested = { onEvent(VideoDownloadState.Event.NotificationPermissionRequested) },
    )
}
