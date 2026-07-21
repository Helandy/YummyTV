package su.afk.yummy.tv.feature.videodownload.mobile.view

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.SaveAlt
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import su.afk.yummy.tv.domain.videodownload.model.VideoDownloadItem
import su.afk.yummy.tv.domain.videodownload.model.VideoDownloadStatus
import su.afk.yummy.tv.domain.videodownload.model.VideoExportStatus
import su.afk.yummy.tv.feature.videodownload.mobile.R
import su.afk.yummy.tv.feature.videodownload.mobile.utils.formatMegabytesOrNull
import kotlin.math.roundToInt

private val DownloadActiveColor = Color(0xFF4CAF50)

@Composable
internal fun VideoDownloadMobileCard(
    item: VideoDownloadItem,
    onClick: () -> Unit,
    onDetailsClick: () -> Unit,
    onDelete: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onRestart: () -> Unit,
    onExport: () -> Unit,
    onCancelExport: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val imageUrl = item.screenshotUrl.ifBlank { item.posterUrl }
    val statusText = item.statusText()
    val diskSizeText = item.diskSizeText()
    val statusColor = when {
        item.status == VideoDownloadStatus.Failed ||
                item.exportStatus == VideoExportStatus.Failed -> MaterialTheme.colorScheme.error

        else -> DownloadActiveColor
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(enabled = item.status == VideoDownloadStatus.Downloaded, onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .weight(0.42f)
                    .aspectRatio(1.45f)
                    .background(MaterialTheme.colorScheme.surface),
                contentAlignment = Alignment.Center,
            ) {
                AsyncImage(
                    model = imageUrl.takeIf { it.isNotBlank() },
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
                Text(
                    text = item.episode,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                        .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(4.dp))
                        .padding(horizontal = 7.dp, vertical = 3.dp),
                )
                if (item.hasProgressIndicator) {
                    LinearProgressIndicator(
                        progress = { item.visibleProgress.coerceIn(0f, 1f) },
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.22f),
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .height(3.dp),
                    )
                }
            }

            Row(
                modifier = Modifier
                    .weight(0.58f)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    Text(
                        text = item.animeTitle,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = stringResource(
                            R.string.video_download_item_dubbing,
                            item.dubbingLabel()
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.82f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = stringResource(
                            R.string.video_download_item_quality_balancer,
                            item.playerName.balancerLabel(),
                            item.qualityLabel,
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.82f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (!statusText.isNullOrBlank()) {
                        Text(
                            text = statusText,
                            style = MaterialTheme.typography.labelSmall,
                            color = statusColor,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    if (!diskSizeText.isNullOrBlank()) {
                        Text(
                            text = diskSizeText,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.82f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }

                Column(
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    IconButton(
                        onClick = onDetailsClick,
                        enabled = item.animeId > 0,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Info,
                            contentDescription = stringResource(R.string.video_download_open_details),
                        )
                    }
                    if (item.status == VideoDownloadStatus.Failed) {
                        IconButton(onClick = onRestart) {
                            Icon(
                                imageVector = Icons.Filled.RestartAlt,
                                contentDescription = stringResource(R.string.video_download_restart),
                            )
                        }
                    }
                    if (item.status.canPause) {
                        IconButton(onClick = onPause) {
                            Icon(
                                imageVector = Icons.Filled.Pause,
                                contentDescription = stringResource(R.string.video_download_pause),
                            )
                        }
                    }
                    if (item.status == VideoDownloadStatus.Paused) {
                        IconButton(onClick = onResume) {
                            Icon(
                                imageVector = Icons.Filled.PlayArrow,
                                contentDescription = stringResource(R.string.video_download_resume),
                            )
                        }
                    }
                    if (item.status == VideoDownloadStatus.Downloaded) {
                        if (item.exportStatus.isActive) {
                            IconButton(onClick = onCancelExport) {
                                Icon(
                                    imageVector = Icons.Filled.Close,
                                    contentDescription = stringResource(
                                        R.string.video_export_cancel,
                                    ),
                                )
                            }
                        } else {
                            IconButton(onClick = onExport) {
                                Icon(
                                    imageVector = Icons.Filled.SaveAlt,
                                    contentDescription = stringResource(
                                        R.string.video_export_item,
                                    ),
                                )
                            }
                        }
                    }
                    IconButton(onClick = onDelete) {
                        Icon(
                            imageVector = Icons.Filled.Delete,
                            contentDescription = stringResource(R.string.video_download_delete),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun VideoDownloadItem.statusText(): String? =
    when {
        status == VideoDownloadStatus.Downloaded && exportStatus == VideoExportStatus.Queued ->
            stringResource(R.string.video_export_status_queued)

        status == VideoDownloadStatus.Downloaded && exportStatus == VideoExportStatus.Preparing ->
            stringResource(
                R.string.video_export_status_preparing,
                (exportProgress.coerceIn(0f, 1f) * 100).roundToInt(),
            )

        status == VideoDownloadStatus.Downloaded && exportStatus == VideoExportStatus.Copying ->
            stringResource(
                R.string.video_export_status_copying,
                (exportProgress.coerceIn(0f, 1f) * 100).roundToInt(),
            )

        status == VideoDownloadStatus.Downloaded && exportStatus == VideoExportStatus.Exported ->
            stringResource(R.string.video_export_status_exported)

        status == VideoDownloadStatus.Downloaded && exportStatus == VideoExportStatus.Failed ->
            exportErrorMessage?.takeIf(String::isNotBlank)
                ?: stringResource(R.string.video_export_status_failed)

        else -> when (status) {
            VideoDownloadStatus.Resolving,
            VideoDownloadStatus.Queued,
            VideoDownloadStatus.Downloading -> {
                val percent = (progress.coerceIn(0f, 1f) * 100).roundToInt()
                stringResource(R.string.video_download_item_progress, percent)
            }

            VideoDownloadStatus.Paused -> {
                val percent = (progress.coerceIn(0f, 1f) * 100).roundToInt()
                stringResource(R.string.video_download_item_paused, percent)
            }

            VideoDownloadStatus.Failed -> errorMessage
                ?.takeIf { it.isNotBlank() }
                ?: stringResource(R.string.video_download_item_error_unknown)

            else -> null
        }
    }

@Composable
private fun VideoDownloadItem.diskSizeText(): String? {
    if (status != VideoDownloadStatus.Downloaded) return null
    val size = bytesDownloaded.formatMegabytesOrNull() ?: return null
    return stringResource(R.string.video_download_item_disk_size, size)
}

private fun VideoDownloadItem.dubbingLabel(): String =
    dubbing.ifBlank { playerName.balancerLabel() }

private fun String.balancerLabel(): String =
    trim()
        .removePrefix("Плеер ")
        .removePrefix("Player ")

private val VideoDownloadItem.hasProgressIndicator: Boolean
    get() = status == VideoDownloadStatus.Resolving ||
            status == VideoDownloadStatus.Queued ||
            status == VideoDownloadStatus.Downloading ||
            status == VideoDownloadStatus.Paused ||
            exportStatus.isActive

private val VideoDownloadItem.visibleProgress: Float
    get() = if (exportStatus.isActive) exportProgress else progress

private val VideoExportStatus.isActive: Boolean
    get() = this == VideoExportStatus.Queued ||
            this == VideoExportStatus.Preparing ||
            this == VideoExportStatus.Copying

private val VideoDownloadStatus.canPause: Boolean
    get() = this == VideoDownloadStatus.Queued ||
            this == VideoDownloadStatus.Downloading
