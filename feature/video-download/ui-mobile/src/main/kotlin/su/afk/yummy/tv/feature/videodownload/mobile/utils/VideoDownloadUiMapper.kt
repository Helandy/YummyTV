package su.afk.yummy.tv.feature.videodownload.mobile.utils

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import su.afk.yummy.tv.domain.videodownload.model.VideoDownloadItem
import su.afk.yummy.tv.domain.videodownload.model.VideoDownloadStatus
import su.afk.yummy.tv.domain.videodownload.model.VideoExportStatus
import su.afk.yummy.tv.feature.videodownload.mobile.R
import kotlin.math.roundToInt

@Composable
internal fun VideoDownloadItem.statusText(): String? =
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
internal fun VideoDownloadItem.diskSizeText(): String? {
    if (status != VideoDownloadStatus.Downloaded) return null
    val size = bytesDownloaded.formatMegabytesOrNull() ?: return null
    return stringResource(R.string.video_download_item_disk_size, size)
}

internal fun VideoDownloadItem.dubbingLabel(): String =
    dubbing.ifBlank { playerName.balancerLabel() }

internal fun String.balancerLabel(): String =
    trim()
        .removePrefix("Плеер ")
        .removePrefix("Player ")

internal val VideoDownloadItem.hasProgressIndicator: Boolean
    get() = status == VideoDownloadStatus.Resolving ||
            status == VideoDownloadStatus.Queued ||
            status == VideoDownloadStatus.Downloading ||
            status == VideoDownloadStatus.Paused ||
            exportStatus.isActive

internal val VideoDownloadItem.visibleProgress: Float
    get() = if (exportStatus.isActive) exportProgress else progress

internal val VideoExportStatus.isActive: Boolean
    get() = this == VideoExportStatus.Queued ||
            this == VideoExportStatus.Preparing ||
            this == VideoExportStatus.Copying

internal val VideoDownloadStatus.canPause: Boolean
    get() = this == VideoDownloadStatus.Queued ||
            this == VideoDownloadStatus.Downloading
