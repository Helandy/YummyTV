package su.afk.yummy.tv.feature.videodownload.mobile.utils

import su.afk.yummy.tv.domain.videodownload.model.VideoDownloadItem
import su.afk.yummy.tv.domain.videodownload.model.VideoDownloadStatus
import su.afk.yummy.tv.domain.videodownload.model.VideoExportStatus

internal fun VideoDownloadItem.isEligibleForExport(destinationUri: String?): Boolean =
    status == VideoDownloadStatus.Downloaded &&
            exportStatus != VideoExportStatus.Queued &&
            exportStatus != VideoExportStatus.Preparing &&
            exportStatus != VideoExportStatus.Copying &&
            !(exportStatus == VideoExportStatus.Exported && exportDirectoryUri == destinationUri)
