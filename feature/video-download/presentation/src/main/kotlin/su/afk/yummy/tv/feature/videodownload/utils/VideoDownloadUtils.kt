package su.afk.yummy.tv.feature.videodownload.utils

import su.afk.yummy.tv.core.common.episode.episodeNumberOrNull
import su.afk.yummy.tv.domain.videodownload.model.VideoDownloadItem
import su.afk.yummy.tv.domain.videodownload.model.VideoDownloadStatus
import su.afk.yummy.tv.domain.videodownload.model.VideoExportStatus

/** Экспорт ещё идёт: в очереди, готовится или копируется. */
internal val VideoExportStatus.isActive: Boolean
    get() = this == VideoExportStatus.Queued ||
        this == VideoExportStatus.Preparing ||
        this == VideoExportStatus.Copying

/** Серия в очереди, качается или выгружается в MP4. */
val VideoDownloadItem.isInProgress: Boolean
    get() = status == VideoDownloadStatus.Resolving ||
        status == VideoDownloadStatus.Queued ||
        status == VideoDownloadStatus.Downloading ||
        exportStatus.isActive

/**
 * Порядок серий внутри сериала: по числовому номеру ("01" и "1" — одна серия),
 * серии без числа — в конец по строке, при равенстве — по id.
 */
internal val episodeSortComparator: Comparator<VideoDownloadItem> =
    compareBy<VideoDownloadItem> { it.episode.episodeNumberOrNull() ?: Double.MAX_VALUE }
        .thenBy { it.episode.trim() }
        .thenBy { it.id }
